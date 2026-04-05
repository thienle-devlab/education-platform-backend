-- ============================================
-- USER SERVICE DATABASE SCHEMA
-- ============================================
-- Database: user_db
-- Version: 1.0
-- Created: 2026-02-07
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- ENUM TYPES
-- ============================================

-- Role type enum
CREATE TYPE role_type AS ENUM ('STUDENT', 'INSTRUCTOR', 'ADMIN', 'MODERATOR');

-- ============================================
-- TABLE: users
-- ============================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    date_of_birth DATE,
    phone_number VARCHAR(20),
    bio TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_users_account_id UNIQUE (account_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_phone_format CHECK (phone_number IS NULL OR phone_number ~* '^\+?[0-9]{10,15}$'),
    CONSTRAINT chk_date_of_birth CHECK (date_of_birth IS NULL OR date_of_birth <= CURRENT_DATE)
);

-- Indexes for users table
CREATE INDEX idx_users_account_id ON users(account_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_full_name ON users(full_name);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Full-text search index for user search
CREATE INDEX idx_users_fulltext ON users USING GIN(to_tsvector('english', full_name || ' ' || COALESCE(email, '')));

-- Comments
COMMENT ON TABLE users IS 'Stores user profile information';
COMMENT ON COLUMN users.account_id IS 'Reference to account in Auth Service (no FK due to microservices)';
COMMENT ON COLUMN users.email IS 'Duplicated from Auth Service for eventual consistency';
COMMENT ON COLUMN users.full_name IS 'User full name';
COMMENT ON COLUMN users.avatar_url IS 'URL to user avatar image';

-- ============================================
-- TABLE: roles
-- ============================================
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name role_type NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_roles_name UNIQUE (name)
);

-- Indexes for roles table
CREATE INDEX idx_roles_name ON roles(name);

-- Comments
COMMENT ON TABLE roles IS 'Defines available roles in the system';
COMMENT ON COLUMN roles.name IS 'Role name: STUDENT, INSTRUCTOR, ADMIN, MODERATOR';

-- ============================================
-- TABLE: user_roles
-- ============================================
CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID,
    
    -- Foreign Keys
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) 
        REFERENCES roles(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- Indexes for user_roles table
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_user_roles_assigned_at ON user_roles(assigned_at);

-- Comments
COMMENT ON TABLE user_roles IS 'Many-to-many relationship between users and roles';
COMMENT ON COLUMN user_roles.assigned_by IS 'User ID who assigned this role (for audit trail)';

-- ============================================
-- TABLE: user_settings
-- ============================================
CREATE TABLE user_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'vi',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_notification BOOLEAN NOT NULL DEFAULT TRUE,
    push_notification BOOLEAN NOT NULL DEFAULT TRUE,
    sms_notification BOOLEAN NOT NULL DEFAULT FALSE,
    theme VARCHAR(20) NOT NULL DEFAULT 'light',
    font_size VARCHAR(20) NOT NULL DEFAULT 'medium',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT uk_user_settings_user_id UNIQUE (user_id),
    CONSTRAINT chk_language CHECK (language IN ('vi', 'en', 'ja', 'ko', 'zh')),
    CONSTRAINT chk_theme CHECK (theme IN ('light', 'dark', 'auto')),
    CONSTRAINT chk_font_size CHECK (font_size IN ('small', 'medium', 'large'))
);

-- Indexes for user_settings table
CREATE INDEX idx_user_settings_user_id ON user_settings(user_id);

-- Comments
COMMENT ON TABLE user_settings IS 'Stores user preferences and settings';
COMMENT ON COLUMN user_settings.language IS 'Preferred language (vi, en, ja, ko, zh)';
COMMENT ON COLUMN user_settings.timezone IS 'User timezone for date/time display';
COMMENT ON COLUMN user_settings.theme IS 'UI theme preference (light, dark, auto)';

-- ============================================
-- TRIGGERS
-- ============================================

-- Auto-update updated_at timestamp for users
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to users table
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to user_settings table
CREATE TRIGGER trigger_user_settings_updated_at
    BEFORE UPDATE ON user_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- FUNCTIONS
-- ============================================

-- Function to create default user settings
CREATE OR REPLACE FUNCTION create_default_user_settings()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_settings (user_id)
    VALUES (NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-create user settings
CREATE TRIGGER trigger_create_user_settings
    AFTER INSERT ON users
    FOR EACH ROW
    EXECUTE FUNCTION create_default_user_settings();

-- Function to assign default role
CREATE OR REPLACE FUNCTION assign_default_role()
RETURNS TRIGGER AS $$
DECLARE
    v_student_role_id UUID;
BEGIN
    -- Get STUDENT role ID
    SELECT id INTO v_student_role_id FROM roles WHERE name = 'STUDENT';
    
    -- Assign STUDENT role to new user
    INSERT INTO user_roles (user_id, role_id)
    VALUES (NEW.id, v_student_role_id);
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-assign default role
CREATE TRIGGER trigger_assign_default_role
    AFTER INSERT ON users
    FOR EACH ROW
    EXECUTE FUNCTION assign_default_role();

-- Function to get user roles
CREATE OR REPLACE FUNCTION get_user_roles(p_user_id UUID)
RETURNS TABLE(role_name role_type, assigned_at TIMESTAMP WITH TIME ZONE) AS $$
BEGIN
    RETURN QUERY
    SELECT r.name, ur.assigned_at
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.id
    WHERE ur.user_id = p_user_id
    ORDER BY ur.assigned_at;
END;
$$ LANGUAGE plpgsql;

-- Function to search users
CREATE OR REPLACE FUNCTION search_users(p_search_term TEXT, p_limit INTEGER DEFAULT 20)
RETURNS TABLE(
    id UUID,
    full_name VARCHAR,
    email VARCHAR,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT u.id, u.full_name, u.email, u.avatar_url, u.created_at
    FROM users u
    WHERE to_tsvector('english', u.full_name || ' ' || u.email) @@ plainto_tsquery('english', p_search_term)
    ORDER BY ts_rank(to_tsvector('english', u.full_name || ' ' || u.email), plainto_tsquery('english', p_search_term)) DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- INITIAL DATA
-- ============================================

-- Insert default roles
INSERT INTO roles (name, description) VALUES
('STUDENT', 'Regular student user with access to courses and learning materials'),
('INSTRUCTOR', 'Instructor who can create and manage courses'),
('ADMIN', 'System administrator with full access to all features'),
('MODERATOR', 'Content moderator who can review and manage user content')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- VIEWS
-- ============================================

-- View for user profiles with roles
CREATE OR REPLACE VIEW user_profiles AS
SELECT 
    u.id,
    u.account_id,
    u.email,
    u.full_name,
    u.avatar_url,
    u.date_of_birth,
    u.phone_number,
    u.bio,
    u.created_at,
    u.updated_at,
    ARRAY_AGG(r.name ORDER BY r.name) as roles,
    us.language,
    us.timezone,
    us.theme
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
LEFT JOIN user_settings us ON u.id = us.user_id
GROUP BY u.id, us.language, us.timezone, us.theme;

-- View for role statistics
CREATE OR REPLACE VIEW role_statistics AS
SELECT 
    r.name as role_name,
    r.description,
    COUNT(ur.user_id) as user_count
FROM roles r
LEFT JOIN user_roles ur ON r.id = ur.role_id
GROUP BY r.id, r.name, r.description
ORDER BY user_count DESC;

-- View for user activity summary
CREATE OR REPLACE VIEW user_summary AS
SELECT 
    COUNT(*) as total_users,
    COUNT(*) FILTER (WHERE created_at > CURRENT_DATE - INTERVAL '7 days') as new_users_last_7_days,
    COUNT(*) FILTER (WHERE created_at > CURRENT_DATE - INTERVAL '30 days') as new_users_last_30_days,
    COUNT(DISTINCT CASE WHEN ur.role_id IN (SELECT id FROM roles WHERE name = 'STUDENT') THEN u.id END) as total_students,
    COUNT(DISTINCT CASE WHEN ur.role_id IN (SELECT id FROM roles WHERE name = 'INSTRUCTOR') THEN u.id END) as total_instructors,
    COUNT(DISTINCT CASE WHEN ur.role_id IN (SELECT id FROM roles WHERE name = 'ADMIN') THEN u.id END) as total_admins
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id;

-- ============================================
-- PERFORMANCE INDEXES
-- ============================================

-- Composite index for common queries
CREATE INDEX idx_users_created_full_name ON users(created_at DESC, full_name);

-- Partial index for users with avatars
CREATE INDEX idx_users_with_avatar ON users(id) WHERE avatar_url IS NOT NULL;

-- ============================================
-- ROW LEVEL SECURITY (Optional - for multi-tenancy)
-- ============================================

-- Enable RLS on users table (commented out by default)
-- ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own profile
-- CREATE POLICY users_select_own ON users
--     FOR SELECT
--     USING (account_id = current_setting('app.current_account_id', TRUE)::UUID);

-- Policy: Users can update their own profile
-- CREATE POLICY users_update_own ON users
--     FOR UPDATE
--     USING (account_id = current_setting('app.current_account_id', TRUE)::UUID);

-- ============================================
-- MATERIALIZED VIEWS (for reporting)
-- ============================================

-- Materialized view for user statistics (refresh daily)
CREATE MATERIALIZED VIEW user_statistics_daily AS
SELECT 
    DATE(created_at) as registration_date,
    COUNT(*) as new_users,
    COUNT(*) FILTER (WHERE email LIKE '%@gmail.com') as gmail_users,
    COUNT(*) FILTER (WHERE email LIKE '%@yahoo.com') as yahoo_users
FROM users
GROUP BY DATE(created_at)
ORDER BY registration_date DESC;

-- Index for materialized view
CREATE INDEX idx_user_statistics_daily_date ON user_statistics_daily(registration_date);

-- Refresh function
CREATE OR REPLACE FUNCTION refresh_user_statistics()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY user_statistics_daily;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- PARTITIONING (Optional - for large datasets)
-- ============================================

-- Example: Partition users table by created_at (year)
-- Uncomment if you expect millions of users

-- CREATE TABLE users_2024 PARTITION OF users
--     FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

-- CREATE TABLE users_2025 PARTITION OF users
--     FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

-- CREATE TABLE users_2026 PARTITION OF users
--     FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

-- ============================================
-- COMPLETION MESSAGE
-- ============================================
DO $$ 
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'User Database Schema Created Successfully!';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Database: user_db';
    RAISE NOTICE 'Tables Created: 4';
    RAISE NOTICE '  - users';
    RAISE NOTICE '  - roles';
    RAISE NOTICE '  - user_roles';
    RAISE NOTICE '  - user_settings';
    RAISE NOTICE 'Views Created: 3';
    RAISE NOTICE 'Materialized Views: 1';
    RAISE NOTICE 'Functions Created: 4';
    RAISE NOTICE 'Triggers Created: 3';
    RAISE NOTICE 'Default Roles Inserted: 4';
    RAISE NOTICE '  - STUDENT';
    RAISE NOTICE '  - INSTRUCTOR';
    RAISE NOTICE '  - ADMIN';
    RAISE NOTICE '  - MODERATOR';
    RAISE NOTICE '============================================';
END $$;
