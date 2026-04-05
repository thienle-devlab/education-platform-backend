package com.lethien.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Login Session entity - tracks user login sessions from different devices
 * Maps to: login_sessions table in auth_db
 */
@Entity
@Table(name = "login_sessions", indexes = {
        @Index(name = "idx_login_sessions_account_id", columnList = "account_id"),
        @Index(name = "idx_login_sessions_is_active", columnList = "is_active"),
        @Index(name = "idx_login_sessions_last_login_at", columnList = "last_login_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_login_session_account"))
    private Account account;

    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "last_login_at", nullable = false)
    private ZonedDateTime lastLoginAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "logout_at")
    private ZonedDateTime logoutAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.lastLoginAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Update last login timestamp
     */
    public void updateLastLogin() {
        this.lastLoginAt = ZonedDateTime.now();
    }

    /**
     * Deactivate session (logout)
     */
    public void deactivate() {
        this.isActive = false;
        this.logoutAt = ZonedDateTime.now();
    }

    /**
     * Get device type from user agent
     */
    public String getDiviceType(){
        if (deviceInfo == null) return "Unknown";

        String lower = deviceInfo.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")){
            return "Mobile";
        } else if (lower.contains("tablet") || lower.contains("ipad")) {
            return "Tablet";
        }else {
            return "Desktop";
        }
    }
}
