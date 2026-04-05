package com.lethien.auth_service.repository;

import com.lethien.auth_service.entity.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for LoginSession entity
 */
@Repository
public interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {

    /**
     * Find all active sessions for an account
     */
    @Query("SELECT ls FROM LoginSession ls WHERE ls.account.id = :accountId AND ls.isActive = true " +
            "ORDER BY ls.lastLoginAt DESC")
    List<LoginSession> findActiveSessionsByAccountId(@Param("accountId") UUID accountId);

    /**
     * Find all sessions for an account (active and inactive)
     */
    List<LoginSession> findByAccountIdOrderByLastLoginAtDesc(UUID accountId);

    /**
     * Deactivate all sessions for an account (logout from all devices)
     */
    @Modifying
    @Query("UPDATE LoginSession ls SET ls.isActive = false, ls.logoutAt = :now " +
            "WHERE ls.account.id = :accountId AND ls.isActive = true")
    int deactivateAllSessionsByAccountId(@Param("accountId") UUID accountId,
                                         @Param("now")ZonedDateTime now);

    /**
     * Count active sessions for an account
     */
    long countByAccountIdAndIsActive(UUID accountId, Boolean isActive);

    /**
     * Delete old inactive sessions (cleanup)
     */
    @Modifying
    @Query("DELETE FROM LoginSession ls WHERE ls.isActive = false " +
            "AND ls.logoutAt < :cutoffDate")
    int deleteOldInactiveSessions(@Param("cutoffDate") ZonedDateTime cutoffDate);
}
