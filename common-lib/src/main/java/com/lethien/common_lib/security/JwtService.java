package com.lethien.common_lib.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * Service for JWT token generation and validation
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * Generate access token
     */
    public String generateAccessToken(UUID accountId, String email) {
        return generateToken(accountId, email, jwtExpiration);
    }

    /**
     * Generate refresh token
     */

    public String generateRefreshToken(UUID accountId, String email) {
        log.info("=== generateRefreshToken called, refreshExpiration={}", refreshExpiration);
        return generateToken(accountId, email, refreshExpiration);
    }

    /**
     * Generate refresh token with custom expiration (for "Remember Me")
     */
    public String generateRefreshToken(UUID accountId, String email, Long customExpiration) {
        log.info("=== generateAccessToken called, jwtExpiration={}", jwtExpiration);
        return generateToken(accountId, email, customExpiration);
    }

    /**
     * Generate JWT token
     */
    private String generateToken(UUID accountId, String email, Long expiration) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expiryDate = now.plusSeconds(expiration / 1000);

        return Jwts.builder()
                .subject(email)
                .claim("accountId", accountId.toString())
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(expiryDate.toInstant()))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract account ID from token
     */
    public UUID getAccountIdFromToken(String token) {
        String accountIdStr = getClaims(token).get("accountId", String.class);
        return UUID.fromString(accountIdStr);
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            return  expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Get remaining time until expiration (in milliseconds)
     */
    public Long getExpirationTime(String token) {
        Date expiration = getClaims(token).getExpiration();
        Date now = new Date();
        return expiration.getTime() - now.getTime();
    }

    /**
     * Parse and get claims from token (NEW API)
     */
    private Claims getClaims(String token) {
        return  Jwts.parser()
        // ✅ NEW: Use verifyWith() instead of setSigningKey()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token) // ✅ NEW: parseSignedClaims instead of parseClaimsJws
                .getPayload(); // ✅ NEW: getPayload() instead of getBody()
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
