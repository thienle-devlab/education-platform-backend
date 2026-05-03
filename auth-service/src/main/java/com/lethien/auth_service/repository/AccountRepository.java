package com.lethien.auth_service.repository;

import com.lethien.auth_service.entity.Account;
import com.lethien.auth_service.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Account entity
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    /**
     * Find account by email
     */
    Optional<Account> findByEmail(String email);

    /**
     * Find account by email (case-insensitive)
     */
    @Query("SELECT a FROM Account a WHERE LOWER(a.email) = LOWER(:email)")
    Optional<Account> findByEmailIgnoreCase(@Param("email") String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if email exists (case-insensitive)
     */
    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE LOWER(a.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    /**
     * Find email change token
     */
    Optional<Account> findByEmailChangeToken(String token);

    boolean existsByPendingEmail(String pendingEmail);

    /**
     * Find account by verification token
     */
    Optional<Account> findByVerificationToken(String verificationToken);

    /**
     * Find active accounts by status
     */
    List<Account> findByStatus(AccountRepository status);

    /**
     * Find accounts by status and email verified
     */
    List<Account> findByStatusAndEmailVerified(AccountStatus status, Boolean emailVerified);

    /**
     * Find pending accounts with expired verification tokens
     */
    @Query("SELECT a FROM Account a WHERE a.status = 'PENDING'" +
            " AND a.emailVerified = false" +
            " AND a.verificationExpiredAt < :now")
    List<Account> findExpiredPendingAccounts(@Param("now")ZonedDateTime now);

    /**
     * Count accounts by status
     */
    long countByStatus(AccountStatus status);

    /**
     * Count verified accounts
     */
    long countByEmailVerified(Boolean emailVerified);
}
