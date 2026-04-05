package com.lethien.auth_service.repository;

import com.lethien.auth_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all valid (non-revoked, non-expired) tokens for an account
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.account.id = :accountId " +
            "AND rt.revoked = false AND rt.expiredAt > :now")
    List<RefreshToken> findValidTokensByAccountId(@Param("accountId") UUID accountId,
                                                  @Param("now")ZonedDateTime now);

    /**
     * Find all tokens for an account
     */
    List<RefreshToken> findByAccountId(UUID accountId);

    /**
     * Revoke all tokens for an account (logout from all devices)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now " +
            "WHERE rt.account.id = :accountId AND rt.revoked = false")
    int revokeAllTokensByAccountId(@Param("accountId") UUID accountId,
                                   @Param("now") ZonedDateTime now);

    /**
     * Delete expired and revoked tokens (cleanup)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiredAt < :now OR rt.revoked = true")
    int deleteExpiredAndRevokedTokens(@Param("now") ZonedDateTime now);

    /**
     * Count active tokens for an account
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.account.id = :accountId " +
            "AND rt.revoked = false AND rt.expiredAt > :now")
    long countActiveTokensByAccountId(@Param("accountId") UUID accountId,
                                      @Param("now") ZonedDateTime now);
}
