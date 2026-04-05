package com.lethien.auth_service.repository;

import com.lethien.auth_service.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LoginAttempt entity
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /**
     * Find login attempt by email
     */
    Optional<LoginAttempt> findByEmail(String email);

    /**
     * Delete login attempts that are no longer locked
     * (cleanup after lock period expired)
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.lockedUntil < :now OR la.lockedUntil IS NULL")
    int deleteUnlockedAttempts(@Param("now")ZonedDateTime now);
}
