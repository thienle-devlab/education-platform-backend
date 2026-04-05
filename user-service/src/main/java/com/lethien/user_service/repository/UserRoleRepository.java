package com.lethien.user_service.repository;

import com.lethien.user_service.entity.RoleType;
import com.lethien.user_service.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    // ============================================
    // FIND ALL ROLES OF A USER
    // ============================================
    /**
     * Find all role assignments for a specific user.
     * Returns UserRole (not Role) — includes assignedAt, assignedBy metadata.
     *
     * Used in: building JWT claims, permission checks, profile display.
     */
    @Query("""
        SELECT ur FROM UserRole ur
        JOIN FETCH ur.role r
        WHERE ur.user.id = :userId
        ORDER BY ur.assignedAt ASC
        """)
    List<UserRole> findAllByUserId(@Param("userId") UUID userId);

    /**
     * Find all RoleType names for a user — lightweight version.
     * Returns only the enum values, no entity hydration.
     *
     * Used in: JWT token generation where only role names are needed.
     */
    @Query("""
        SELECT r.name FROM UserRole ur
        JOIN ur.role r
        WHERE ur.user.id = :userId
        """)
    List<RoleType> findRoleTypesByUserId(@Param("userId") UUID userId);

    // ============================================
    // CHECK IF USER HAS A SPECIFIC ROLE
    // ============================================

    /**
     * Check if a user has a specific role.
     * Used in: authorization checks, @PreAuthorize guards.
     */
    @Query("""
        SELECT COUNT(ur) > 0 FROM UserRole ur
        JOIN ur.role r
        WHERE ur.user.id = :userId
          AND r.name = :roleType
        """)
    boolean existsByUserIdAndRoleType(
            @Param("userId") UUID userId,
            @Param("roleType") RoleType roleType);

    /**
     * Find a specific role assignment for a user.
     * Used in: checking before assigning (avoid duplicate), or before revoking.
     */
    @Query("""
        SELECT ur FROM UserRole ur
        JOIN ur.role r
        WHERE ur.user.id = :userId
          AND r.name = :roleType
        """)
    Optional<UserRole> findByUserIdAndRoleType(
            @Param("userId") UUID userId,
            @Param("roleType") RoleType roleType);

    // ============================================
    // FIND ALL USERS OF A ROLE
    // ============================================

    /**
     * Find all user IDs that have a specific role.
     * Returns UUID list — lightweight, avoids loading full UserProfile.
     *
     * Used in: bulk operations, notifications to all instructors/admins.
     */
    @Query("""
        SELECT ur.user.id FROM UserRole ur
        JOIN ur.role r
        WHERE r.name = :roleType
        """)
    List<UUID> findUserIdsByRoleType(@Param("roleType") RoleType roleType);

    /**
     * Find all UserRole entries for a specific role.
     * Returns full UserRole including user + metadata.
     *
     * Used in: admin panel showing who has a specific role and when it was assigned.
     */
    @Query("""
        SELECT ur FROM UserRole ur
        JOIN FETCH ur.user u
        JOIN FETCH ur.role r
        WHERE r.name = :roleType
        ORDER BY ur.assignedAt DESC
        """)
    List<UserRole> findAllByRoleType(@Param("roleType") RoleType roleType);

    // ============================================
    // FIND BY ASSIGNED BY
    // ============================================

    /**
     * Find all role assignments made by a specific admin/user.
     * Used in: audit trail — "show all roles assigned by admin X".
     */
    List<UserRole> findByAssignedBy(UUID assignedBy);

    /**
     * Find all role assignments made by a specific admin for a specific role type.
     * Used in: detailed audit — "which users did admin X assign as INSTRUCTOR?".
     */
    @Query("""
        SELECT ur FROM UserRole ur
        JOIN ur.role r
        WHERE ur.assignedBy = :assignedBy
          AND r.name = :roleType
        ORDER BY ur.assignedAt DESC
        """)
    List<UserRole> findByAssignedByAndRoleType(
            @Param("assignedBy") UUID assignedBy,
            @Param("roleType") RoleType roleType);

    // ============================================
    // DELETE
    // ============================================

    /**
     * Remove a specific role from a user.
     * @Modifying required for DELETE/UPDATE JPQL queries.
     *
     * Used in: revoke role flow.
     */
    @Modifying
    @Query("""
        DELETE FROM UserRole ur
        WHERE ur.user.id = :userId
          AND ur.role.id = :roleId
        """)
    void deleteByUserIdAndRoleId(
            @Param("userId") UUID userId,
            @Param("roleId") UUID roleId);

    /**
     * Remove all roles from a user.
     * Used in: user suspension or account cleanup.
     */
    @Modifying
    @Query("""
        DELETE FROM UserRole ur
        WHERE ur.user.id = :userId
        """)
    void deleteAllByUserId(@Param("userId") UUID userId);
}
