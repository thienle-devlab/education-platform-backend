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
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JwtService — generate và validate JWT tokens.
 *
 * Thay đổi so với version cũ:
 *   + generateAccessToken() nhận thêm Set<String> roles
 *   + roles được lưu vào JWT claim "roles"
 *   + getRolesFromToken() — extract roles từ JWT
 *
 * Backward compatible:
 *   generateAccessToken(accountId, email) vẫn giữ để
 *   refresh token flow không cần roles (lấy từ DB lúc refresh).
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

    // ============================================
    // GENERATE ACCESS TOKEN
    // ============================================

    /**
     * Generate access token WITH roles — dùng khi login.
     * roles được lưu vào claim "roles" dạng List<String>.
     *
     * @param accountId accountId từ auth_db
     * @param email     email của user
     * @param roles     set role names: ["STUDENT"], ["INSTRUCTOR"], ["ADMIN"]
     */
    public String generateAccessToken(UUID accountId, String email, Set<String> roles) {
        return generateToken(accountId, email, roles, jwtExpiration);
    }

    /**
     * Generate access token WITHOUT roles — dùng khi refresh token.
     * Refresh token flow sẽ lấy roles từ User Service và gọi overload trên.
     *
     * @deprecated Dùng generateAccessToken(accountId, email, roles) khi có roles
     */

    public String generateAccessToken(UUID accountId, String email) {
        return generateToken(accountId, email, Set.of(), jwtExpiration);
    }

    // ============================================
    // GENERATE REFRESH TOKEN
    // ============================================
    public String generateRefreshToken(UUID accountId, String email) {
        log.info("=== generateRefreshToken called, refreshExpiration={}", refreshExpiration);
        return generateToken(accountId, email, Set.of(), refreshExpiration);
    }

    public String generateRefreshToken(UUID accountId, String email, Long customExpiration) {
        log.info("=== generateAccessToken called, jwtExpiration={}", jwtExpiration);
        return generateToken(accountId, email, Set.of(), customExpiration);
    }

    // ============================================
    // EXTRACT CLAIMS
    // ============================================
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
     * Extract roles từ JWT claim "roles".
     * Trả về empty Set nếu token không có roles claim
     * (backward compatible với token cũ không có roles).
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        try {
            Object rolesClaim = getClaims(token).get("roles");
            if (rolesClaim instanceof List<?> list) {
                return Set.copyOf((List<String>) list);
            }
            return Set.of();
        } catch (Exception e) {
            log.warn("Could not extract roles from token: {}", e.getMessage());
            return Set.of();
        }
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

    // ============================================
    // PRIVATE
    // ============================================
    /**
     * Generate JWT token
     */
    private String generateToken(UUID accountId, String email, Set<String> roles, Long expiration) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expiryDate = now.plusSeconds(expiration / 1000);

        var builder =  Jwts.builder()
                .subject(email)
                .claim("accountId", accountId.toString())
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(expiryDate.toInstant()))
                .signWith(getSigningKey());

        // Chỉ thêm roles claim khi có roles — giữ token nhỏ gọn
        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", List.copyOf(roles));
        }

        return builder.compact();
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
