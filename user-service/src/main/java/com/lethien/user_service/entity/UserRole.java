package com.lethien.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * UserRole entity - many-to-many relationship between users and roles
 * Maps to: user_roles table in user_db
 */
@Entity
@Table(name = "user_roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role_id"})
}, indexes = {
        @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
        @Index(name = "idx_user_roles_role_id", columnList = "role_id"),
        @Index(name = "idx_user_roles_assigned_at", columnList = "assigned_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role_user"))
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role_role"))
    private Role role;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private ZonedDateTime assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.assignedAt = ZonedDateTime.now();
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Check if role was auto-assigned (system)
     */
    public boolean isAutoAssigned(){
        return this.assignedBy == null;
    }

    /**
     * Get role name
     */
    public RoleType getRoleName() {
        return this.role != null ? this.role.getName() : null;
    }
}
