package com.lethien.user_service.security;

import com.lethien.common_lib.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JwtAuthenticationFilter — intercepts every request and validates JWT.
 *
 * Flow:
 *   1. Extract Bearer token from Authorization header
 *   2. Validate token signature + expiry using shared secret
 *   3. Extract accountId + email from claims
 *   4. Set Authentication in SecurityContext
 *   5. Continue filter chain
 *
 * User Service does NOT call Auth Service to verify —
 * verification is done locally with the shared secret key.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && jwtService.validateToken(token)) {
            try {
                UUID accountId = jwtService.getAccountIdFromToken(token);
                String email = jwtService.getEmailFromToken(token);

                // User Service has no roles in JWT currently —
                // roles are managed locally in user_db.
                // Using ROLE_USER as a base authority for all authenticated users.
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_USER"));

                // Build authentication with accountId as principal
                // Controllers can access accountId via:
                //   SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(accountId, email, authorities);

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                log.debug("JWT authenticated: accountId={}, email={}", accountId, email);
            } catch (Exception e) {
                log.error("Failed to set authentication from JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract Bearer token from Authorization header.
     * Expected format: "Authorization: Bearer <token>"
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
