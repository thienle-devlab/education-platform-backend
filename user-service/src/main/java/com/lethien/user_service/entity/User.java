package com.lethien.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User entity - stores user profile information
 * Maps to: users table in user_db
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_full_name", columnList = "full_name"),
        @Index(name = "idx_users_created_at", columnList = "created_at"),
        @Index(name = "idx_users_account_id", columnList = "account_id"),
        @Index(name = "idx_users_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // ============================================
    // RELATIONSHIPS
    // ============================================

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserSetting userSetting;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Add role to user
     */
    public void addRole(Role role, UUID assignedBy){
        UserRole userRole = UserRole.builder()
                .user(this)
                .role(role)
                .assignedBy(assignedBy)
                .build();
        this.userRoles.add(userRole);
    }

    /**
     * Remove role from user
     */
    public void removedRole(Role role){
        this.userRoles.removeIf(ur -> ur.getRole(). equals(role));
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(RoleType roleType){
        return this.userRoles.stream()
                .anyMatch(ur -> ur.getRole().getName() == roleType);
    }

    /**
     * Get all role types
     */
    public Set<RoleType> getRoleTypes(){
        Set<RoleType> roles = new HashSet<>();
        for (UserRole ur : this.userRoles) {
            roles.add(ur.getRole().getName());
        }
        return roles;
    }

    /**
     * Check if user is an instructor
     */
    public boolean isInstructor(){
        return hasRole(RoleType.INSTRUCTOR) || hasRole(RoleType.ADMIN);
    }

    /**
     * Check if user is an admin
     */
    public boolean isAdmin(){
        return hasRole(RoleType.ADMIN);
    }

    /**
     * Get age from date of birth
     */
    public Integer getAge(){
        if (this.dateOfBirth == null) return  null;
        return LocalDate.now().getYear() - this.dateOfBirth.getYear();
    }

    /**
     * Get first name from full name
     */
    public String getFirstName(){
        if (this.fullName == null) return "";
        String[] parts = this.fullName.trim().split("\\s+");
        return parts[0];
    }

    /**
     * Get last name from full name
     */
     public String getLastName(){
         if (this.fullName == null) return "";
         String[] parts = this.fullName.trim().split("\\s+");
         return parts.length > 1 ? parts[parts.length - 1] : "";
     }


}
