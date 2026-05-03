package com.lethien.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.annotations.JdbcType;
//import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Account entity - stores user authentication accounts
 * Maps to: accounts table in auth_db
 */
@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_email", columnList = "email"),
        @Index(name = "idx_accounts_status", columnList = "status"),
        @Index(name = "idx_accounts_status_email_verified", columnList = "status, email_verified"),
        @Index(name = "idx_accounts_verification_token", columnList = "verification_token"),
        @Index(name = "idx_accounts_email_change_token", columnList = "email_change_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

//    @Enumerated(EnumType.STRING)
//    @JdbcType(PostgreSQLEnumJdbcType.class)
//    @Column(name = "status", nullable = false, columnDefinition = "account_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "account_status")
    private AccountStatus status;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;

//    @Column(name = "verification_token", length = 255)
    @Column(name = "verification_token", columnDefinition = "TEXT")
    private String verificationToken;

    @Column(name = "verification_expired_at")
    private ZonedDateTime verificationExpiredAt;

    @Column(name = "pending_email", length = 255)
    private String pendingEmail;

    @Column(name = "email_change_token", columnDefinition = "TEXT")
    private String emailChangeToken;

    @Column(name = "email_change_expired_at")
    private ZonedDateTime emailChangeExpiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void  onCreate(){
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        //Set default
        if (this.emailVerified == null){
            this.emailVerified = false;
        }
        if (this.status == null){
            this.status = AccountStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = ZonedDateTime.now();
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Check if account is active and verified
     */
    public boolean isActive(){
        return AccountStatus.ACTIVE.equals(this.status) && Boolean.TRUE.equals(this.emailVerified);
    }

    /**
     * Check if account is locked
     */
    public boolean isLocked(){
        return AccountStatus.LOCKED.equals(this.status);
    }

    /**
     * Activate account after email verification
     */
    public void activate(){
        this.status = AccountStatus.ACTIVE;
        this.emailVerified = true;
        this.verificationToken = null;
        this.verificationExpiredAt = null;
    }

    /**
     * Lock account (e.g., after multiple failed login attempts)
     */
    public void lock() {
        this.status = AccountStatus.LOCKED;
    }

    /**
     * Suspend account (admin action)
     */
    public void suspend() {
        this.status = AccountStatus.SUSPENDED;
    }

    /**
     * Bắt đầu flow đổi email — lưu pending email + token.
     * Gọi trong AuthService.requestEmailChange()
     */
    public void requestEmailChange(String newEmail, String token, ZonedDateTime expireeAt) {
        this.pendingEmail = newEmail;
        this.emailChangeToken = token;
        this.emailChangeExpiredAt = expireeAt;
    }

    /**
     * Xác nhận đổi email sau khi user click link.
     * Email chính thức được cập nhật, clear toàn bộ pending state.
     * Gọi trong AuthService.confirmEmailChange()
     */
    public void confirmEmailChange() {
        this.email = this.pendingEmail;
        this.pendingEmail = null;
        this.emailChangeToken     = null;
        this.emailChangeExpiredAt = null;
        // emailVerified giữ nguyên true — user đã xác nhận qua link
    }

    /**
     * Huỷ yêu cầu đổi email (user chủ động huỷ hoặc token hết hạn).
     * Gọi trong AuthService.cancelEmailChange() và confirmEmailChange() khi expired
     */
    public void cancelEmailChange() {
        this.pendingEmail = null;
        this.emailChangeToken = null;
        this.emailChangeExpiredAt = null;
    }

    /**
     * Kiểm tra có đang pending đổi email không.
     * Dùng để validate trước khi resend hoặc cancel
     */
    public boolean hasEmailChangePending() {
        return this.pendingEmail != null && this.emailChangeToken != null;
    }

    /**
     * Kiểm tra token đổi email còn hạn không.
     * Tương tự logic check verificationExpiredAt trong AuthService.verifyEmail()
     */
    public boolean isEmailChangeTokenExpired() {
        return this.emailChangeExpiredAt == null ||
                this.emailChangeExpiredAt.isBefore(ZonedDateTime.now());
    }
}
