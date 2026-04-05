package com.lethien.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Login Attempt entity - tracks failed login attempts for rate limiting
 * Maps to: login_attempts table in auth_db
 */
@Entity
@Table(name = "login_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_login_attempt_email", columnNames = {"email"})
        },
        indexes = {
                @Index(name = "idx_login_attempts_account_id", columnList = "account_id"),
                @Index(name = "idx_login_attempts_email", columnList = "email"),
                @Index(name = "idx_login_attempts_locked_until", columnList = "locked_until")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", foreignKey = @ForeignKey(name = "fk_login_attempt_account"))
    private Account account;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_attempt_at", nullable = false)
    private ZonedDateTime lastAttemptAt;

    @Column(name = "locked_until")
    private ZonedDateTime lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // ============================================
    // CONSTANTS
    // ============================================

    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_DURATION_MINUTES = 30;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate(){
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastAttemptAt = now;
        if (this.attemptCount == null) {
            this.attemptCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Increment failed attempt count
     */
    public void incrementAttempt(){
        this.attemptCount++;
        this.lastAttemptAt = ZonedDateTime.now();

        // Lock account if max attempts reached
        if (this.attemptCount >= MAX_ATTEMPTS) {
            this.lockedUntil = ZonedDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        }
    }

    /**
     * Reset attempts after successful login
     */
    public void reset() {
        this.attemptCount = 0;
        this.lockedUntil = null;
    }

    /**
     * Check if account is currently locked
     */
    public boolean isLocked(){
        return this.lockedUntil !=null && this.lockedUntil.isAfter(ZonedDateTime.now());
    }

    /**
     * Get remaining lock time in minutes
     */
    public long getRemainingLockMinutes(){
        if (!isLocked()) return 0;
        return java.time.Duration.between(ZonedDateTime.now(), this.lockedUntil).toMinutes();
    }
}
