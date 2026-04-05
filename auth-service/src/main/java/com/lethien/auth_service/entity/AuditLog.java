package com.lethien.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Log entity - audit trail for all authentication-related actions
 * Maps to: audit_logs table in auth_db
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_account_id", columnList = "account_id"),
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_logs_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_audit_log_account"))
    private Account account;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private ZonedDateTime timestamp;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.timestamp = ZonedDateTime.now();
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Add metadata entry
     */
    public void addMetadata(String key, Object value){
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Get metadata value
     */
    public Object getMetadata(String key){
        return this.metadata != null ? this.metadata.get(key) : null;
    }

    // ============================================
    // COMMON ACTIONS
    // ============================================

    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_EMAIL_VERIFY = "EMAIL_VERIFY";
    public static final String ACTION_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ACTION_OAUTH_LINK = "OAUTH_LINK";
    public static final String ACTION_OAUTH_UNLINK = "OAUTH_UNLINK";
    public static final String ACTION_ACCOUNT_LOCK = "ACCOUNT_LOCK";
    public static final String ACTION_ACCOUNT_UNLOCK = "ACCOUNT_UNLOCK";
    public static final String ACTION_ACCOUNT_SUSPEND = "ACCOUNT_SUSPEND";
}
