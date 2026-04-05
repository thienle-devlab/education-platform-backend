package com.lethien.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Refresh Token entity - stores refresh tokens for JWT authentication
 * Maps to: refresh_tokens table in auth_db
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_account_id", columnList = "account_id"),
        @Index(name = "idx_refresh_tokens_token", columnList = "token"),
        @Index(name = "idx_refresh_tokens_expired_at", columnList = "expired_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_token_account"))
    private Account account;

//    @Column(name = "token", nullable = false, unique = true, length = 255)
    @Column(name = "token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    @Column(name = "expired_at", nullable = false)
    private ZonedDateTime expiredAt;

    @Column(name = "revoked", nullable = false)
    private Boolean revoked;

    @Column(name = "revoked_at")
    private ZonedDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
        if (this.revoked == null) {
            this.revoked = false;
        }
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Check if token is valid (not expired and not revoked)
     */
    public boolean isValid(){
        return !isExpired() && !Boolean.TRUE.equals((this.revoked));
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired(){
        return this.expiredAt != null && this.expiredAt.isBefore(ZonedDateTime.now());
    }

    /**
     * Revoke this token
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = ZonedDateTime.now();
    }
}
