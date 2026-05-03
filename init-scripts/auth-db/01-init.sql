-- ============================================
-- AUTH SERVICE DATABASE SCHEMA
-- ============================================
-- Database: auth_db
-- Version: 1.0
-- Created: 2026-02-07
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- ENUM TYPES
-- ============================================

-- Account status enum
CREATE TYPE account_status AS ENUM ('PENDING', 'ACTIVE', 'LOCKED', 'SUSPENDED');

-- OAuth provider enum
CREATE TYPE oauth_provider AS ENUM ('GOOGLE', 'FACEBOOK', 'GITHUB');

-- ============================================
-- TABLE: accounts
-- ============================================
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    status account_status NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token TEXT, -- (VARCHAR(255) -> TEXT)
    verification_expired_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_accounts_email UNIQUE (email),
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Alter table
ALTER TABLE accounts
    ADD COLUMN pending_email          VARCHAR(255),
    ADD COLUMN email_change_token     TEXT,
    ADD COLUMN email_change_expired_at TIMESTAMP WITH TIME ZONE;

-- Indexes for accounts table
CREATE INDEX idx_accounts_email ON accounts(email);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_verification_token ON accounts(verification_token) WHERE verification_token IS NOT NULL;

-- Create index for email_change_token
CREATE INDEX idx_accounts_email_change_token
    ON accounts(email_change_token)
    WHERE email_change_token IS NOT NULL;

-- Comments
COMMENT ON TABLE accounts IS 'Stores user authentication accounts';
COMMENT ON COLUMN accounts.email IS 'User email address (unique)';
COMMENT ON COLUMN accounts.password_hash IS 'BCrypt hashed password (nullable for OAuth-only accounts)';
COMMENT ON COLUMN accounts.status IS 'Account status: PENDING, ACTIVE, LOCKED, SUSPENDED';
COMMENT ON COLUMN accounts.email_verified IS 'Whether email has been verified';

-- ============================================
-- TABLE: oauth_identities
-- ============================================
CREATE TABLE oauth_identities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL,
    provider oauth_provider NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_oauth_account FOREIGN KEY (account_id) 
        REFERENCES accounts(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uk_oauth_provider_user UNIQUE (provider, provider_user_id)
);

-- Indexes for oauth_identities table
CREATE INDEX idx_oauth_account_id ON oauth_identities(account_id);
CREATE INDEX idx_oauth_provider ON oauth_identities(provider);

-- Comments
COMMENT ON TABLE oauth_identities IS 'Stores OAuth provider identities linked to accounts';
COMMENT ON COLUMN oauth_identities.provider_user_id IS 'User ID from OAuth provider (e.g., Google user ID)';

-- ============================================
-- TABLE: refresh_tokens
-- ============================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL,
    token TEXT NOT NULL, -- (VARCHAR(255) -> TEXT)
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_refresh_token_account FOREIGN KEY (account_id) 
        REFERENCES accounts(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uk_refresh_token UNIQUE (token),
    CONSTRAINT chk_revoked_at CHECK (
        (revoked = TRUE AND revoked_at IS NOT NULL) OR 
        (revoked = FALSE AND revoked_at IS NULL)
    )
);

-- Indexes for refresh_tokens table
CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens(account_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expired_at ON refresh_tokens(expired_at);

-- Comments
COMMENT ON TABLE refresh_tokens IS 'Stores refresh tokens for JWT authentication';
COMMENT ON COLUMN refresh_tokens.token IS 'UUID refresh token';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Whether token has been revoked (logout)';

-- ============================================
-- TABLE: login_sessions
-- ============================================
CREATE TABLE login_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL,
    device_info TEXT,
    ip_address VARCHAR(45),
    last_login_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    logout_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_login_session_account FOREIGN KEY (account_id) 
        REFERENCES accounts(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_logout_at CHECK (
        (is_active = FALSE AND logout_at IS NOT NULL) OR 
        (is_active = TRUE)
    )
);

-- Indexes for login_sessions table
CREATE INDEX idx_login_sessions_account_id ON login_sessions(account_id);
CREATE INDEX idx_login_sessions_is_active ON login_sessions(is_active);
CREATE INDEX idx_login_sessions_last_login_at ON login_sessions(last_login_at);

-- Comments
COMMENT ON TABLE login_sessions IS 'Tracks user login sessions from different devices';
COMMENT ON COLUMN login_sessions.device_info IS 'User-Agent string from browser';
COMMENT ON COLUMN login_sessions.ip_address IS 'IP address of login (IPv4 or IPv6)';

-- ============================================
-- TABLE: login_attempts
-- ============================================
CREATE TABLE login_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID,
    email VARCHAR(255) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_login_attempt_account FOREIGN KEY (account_id) 
        REFERENCES accounts(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uk_login_attempt_email UNIQUE (email),
    CONSTRAINT chk_attempt_count CHECK (attempt_count >= 0)
);

-- Indexes for login_attempts table
CREATE INDEX idx_login_attempts_account_id ON login_attempts(account_id);
CREATE INDEX idx_login_attempts_email ON login_attempts(email);
CREATE INDEX idx_login_attempts_locked_until ON login_attempts(locked_until) WHERE locked_until IS NOT NULL;

-- Comments
COMMENT ON TABLE login_attempts IS 'Tracks failed login attempts for rate limiting';
COMMENT ON COLUMN login_attempts.attempt_count IS 'Number of consecutive failed attempts';
COMMENT ON COLUMN login_attempts.locked_until IS 'Timestamp until account is locked (NULL if not locked)';

-- ============================================
-- TABLE: audit_logs
-- ============================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSONB,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_audit_log_account FOREIGN KEY (account_id) 
        REFERENCES accounts(id) ON DELETE CASCADE
);

-- Indexes for audit_logs table
CREATE INDEX idx_audit_logs_account_id ON audit_logs(account_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_metadata ON audit_logs USING GIN(metadata);

-- Comments
COMMENT ON TABLE audit_logs IS 'Audit trail for all authentication-related actions';
COMMENT ON COLUMN audit_logs.action IS 'Action performed (e.g., LOGIN, LOGOUT, PASSWORD_CHANGE)';
COMMENT ON COLUMN audit_logs.metadata IS 'Additional JSON metadata about the action';

-- ============================================
-- TRIGGERS
-- ============================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to accounts table
CREATE TRIGGER trigger_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to login_attempts table
CREATE TRIGGER trigger_login_attempts_updated_at
    BEFORE UPDATE ON login_attempts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- FUNCTIONS
-- ============================================

-- Function to clean up expired tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM refresh_tokens 
    WHERE expired_at < CURRENT_TIMESTAMP 
    AND revoked = TRUE;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to reset login attempts
CREATE OR REPLACE FUNCTION reset_login_attempts(p_email VARCHAR)
RETURNS VOID AS $$
BEGIN
    UPDATE login_attempts 
    SET attempt_count = 0, 
        locked_until = NULL,
        updated_at = CURRENT_TIMESTAMP
    WHERE email = p_email;
END;
$$ LANGUAGE plpgsql;

-- Function to increment login attempts
CREATE OR REPLACE FUNCTION increment_login_attempts(p_email VARCHAR)
RETURNS INTEGER AS $$
DECLARE
    v_attempt_count INTEGER;
    v_account_id UUID;
BEGIN
    -- Get account_id if exists
    SELECT id INTO v_account_id FROM accounts WHERE email = p_email;
    
    -- Insert or update login attempt
    INSERT INTO login_attempts (account_id, email, attempt_count, last_attempt_at)
    VALUES (v_account_id, p_email, 1, CURRENT_TIMESTAMP)
    ON CONFLICT (email) DO UPDATE
    SET attempt_count = login_attempts.attempt_count + 1,
        last_attempt_at = CURRENT_TIMESTAMP,
        locked_until = CASE 
            WHEN login_attempts.attempt_count + 1 >= 5 
            THEN CURRENT_TIMESTAMP + INTERVAL '30 minutes'
            ELSE NULL
        END,
        updated_at = CURRENT_TIMESTAMP
    RETURNING attempt_count INTO v_attempt_count;
    
    RETURN v_attempt_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- INITIAL DATA (Optional - for development)
-- ============================================

-- Insert default roles (if needed for reference)
-- Note: This is just for documentation, actual roles are in User Service

-- ============================================
-- GRANTS (Optional - for security)
-- ============================================

-- Grant privileges to auth_user
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO auth_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO auth_user;

-- ============================================
-- VIEWS (Optional - for convenience)
-- ============================================

-- View for active sessions
CREATE OR REPLACE VIEW active_sessions AS
SELECT 
    ls.id,
    ls.account_id,
    a.email,
    ls.device_info,
    ls.ip_address,
    ls.last_login_at,
    ls.created_at
FROM login_sessions ls
JOIN accounts a ON ls.account_id = a.id
WHERE ls.is_active = TRUE
ORDER BY ls.last_login_at DESC;

-- View for account statistics
CREATE OR REPLACE VIEW account_statistics AS
SELECT 
    status,
    COUNT(*) as count,
    COUNT(*) FILTER (WHERE email_verified = TRUE) as verified_count,
    COUNT(*) FILTER (WHERE email_verified = FALSE) as unverified_count
FROM accounts
GROUP BY status;

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================

-- Composite index for common queries
CREATE INDEX idx_accounts_status_email_verified ON accounts(status, email_verified);

-- Partial index for pending verifications
CREATE INDEX idx_accounts_pending_verification 
ON accounts(created_at) 
WHERE status = 'PENDING' AND email_verified = FALSE;

-- Index for token cleanup
CREATE INDEX idx_refresh_tokens_expired_revoked 
ON refresh_tokens(expired_at, revoked) 
WHERE revoked = TRUE;

-- ============================================
-- COMPLETION MESSAGE
-- ============================================
DO $$ 
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Auth Database Schema Created Successfully!';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Database: auth_db';
    RAISE NOTICE 'Tables Created: 6';
    RAISE NOTICE '  - accounts';
    RAISE NOTICE '  - oauth_identities';
    RAISE NOTICE '  - refresh_tokens';
    RAISE NOTICE '  - login_sessions';
    RAISE NOTICE '  - login_attempts';
    RAISE NOTICE '  - audit_logs';
    RAISE NOTICE 'Views Created: 2';
    RAISE NOTICE 'Functions Created: 3';
    RAISE NOTICE 'Triggers Created: 2';
    RAISE NOTICE '============================================';
END $$;
