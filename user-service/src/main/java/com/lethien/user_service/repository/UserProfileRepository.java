package com.lethien.user_service.repository;

import com.lethien.user_service.entity.RoleType;
import com.lethien.user_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<User, UUID> {

    // ============================================
    // FIND BY EMAIL / ACCOUNT ID
    // ============================================

    /**
     * Find user by email.
     * Used in: login flow, email uniqueness check.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by accountId from Auth Service.
     * Used in: inter-service lookup after authentication.
     */
    Optional<User> findByAccountId(UUID accountId);

    /**
     * Check if an email is already taken.
     * Faster than findByEmail() — no entity hydration needed.
     */
    boolean existsByEmail(String email);

    /**
     * Check if an accountId is already registered.
     */
    boolean existsByAccountId(UUID accountId);

    // ============================================
    // FIND BY ROLE
    // ============================================

    /**
     * Find all users that have a specific role, with pagination.
     * Joins through user_roles → roles to filter by RoleType enum.
     *
     * Used in: admin panel listing students, listing instructors, etc.
     */
    @Query("""
        SELECT u FROM User u
        JOIN u.userRoles ur
        JOIN ur.role r
        WHERE r.name = :roleType
        """)
    Page<User> findAllByRole(@Param("roleType") RoleType roleType, Pageable pageable);

    /**
     * Count users by role.
     * Used in: dashboard statistics (total students, total instructors).
     */
    @Query("""
        SELECT COUNT(u) FROM User u
        JOIN u.userRoles ur
        JOIN ur.role r
        WHERE r.name = :roleType
        """)
    long countByRole(@Param("roleType") RoleType roleType);

    // ============================================
    // FULL-TEXT SEARCH
    // ============================================
    /**
     * Full-text search on full_name and email using PostgreSQL tsvector.
     * Leverages the GIN index: idx_users_fulltext defined in 01-init.sql.
     *
     * Uses plainto_tsquery — handles plain search terms without special syntax.
     * Example: searchTerm = "nguyen van" → matches "Nguyen Van An", etc.
     *
     * Used in: admin user search, autocomplete.
     */
    @Query(value = """
        SELECT * FROM users u
        WHERE to_tsvector('english', u.full_name || ' ' || u.email)
              @@ plainto_tsquery('english', :searchTerm)
        ORDER BY ts_rank(
            to_tsvector('english', u.full_name || ' ' || u.email),
            plainto_tsquery('english', :searchTerm)
        ) DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM users u
        WHERE to_tsvector('english', u.full_name || ' ' || u.email)
              @@ plainto_tsquery('english', :searchTerm)
        """,
            nativeQuery = true)
    Page<User> fullTextSearch(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Simple LIKE search — fallback when full-text is overkill
     * or when searchTerm is a partial email/name fragment.
     *
     * Used in: basic search, short queries (< 3 chars).
     */
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ============================================
    // PAGINATION & SORTING (via JpaRepository)
    // ============================================
    @Query("""
        SELECT u FROM User u
        JOIN u.userRoles ur
        JOIN ur.role r
        WHERE r.name = :roleType
          AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR  LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<User> findByRoleAndKeyword(
            @Param("roleType") RoleType roleType,
            @Param("keyword") String keyword,
            Pageable pageable);

}
