package com.lethien.course_service.security;

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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JwtAuthenticationFilter — validate JWT và set authorities từ roles trong token.
 *
 * Thay đổi so với version cũ:
 *   + Extract roles từ JWT claim "roles"
 *   + Map roles → Spring Security authorities (ROLE_STUDENT, ROLE_INSTRUCTOR...)
 *   + Nếu JWT không có roles → fallback ROLE_USER (backward compatible)
 *
 * Sau khi filter này chạy, các endpoint có thể dùng:
 *   @PreAuthorize("hasRole('INSTRUCTOR')")
 *   @PreAuthorize("hasRole('ADMIN')")
 *   SecurityContextHolder.getContext().getAuthentication().getAuthorities()
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

                // Extract roles từ JWT — Set<String> ["STUDENT", "INSTRUCTOR"...]
                Set<String> roles = jwtService.getRolesFromToken(token);

                // Map roles → Spring Security GrantedAuthority với prefix ROLE_
                List<SimpleGrantedAuthority> authorities;

                if (roles.isEmpty()) {
                    // Backward compatible: JWT cũ không có roles → ROLE_USER
                    authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                    log.debug("JWT has no roles claim — using ROLE_USER fallback: accountId={}",
                            accountId);
                } else {
                    authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());
                    log.debug("JWT authenticated: accountId={}, roles={}", accountId, roles);
                }

                // Build authentication with accountId as principal
                // Controllers can access accountId via:
                //   SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(accountId, email, authorities);

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

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
