package com.lethien.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * OAuth Identity entity - stores OAuth provider identities linked to accounts
 * Maps to: oauth_identities table in auth_db
 */
@Entity
@Table(name = "oauth_identities", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_user_id"})
        },
        indexes = {
                @Index(name = "idx_oauth_account_id", columnList = "account_id"),
                @Index(name = "idx_oauth_provider", columnList = "provider")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_oauth_account"))
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, columnDefinition = "oauth_provider")
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate(){
        this.createdAt = ZonedDateTime.now();
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Get provider display name
     */
    public String getProviderDisplayName(){
        return provider != null ? provider.getDisplayName() : "";
    }
}
