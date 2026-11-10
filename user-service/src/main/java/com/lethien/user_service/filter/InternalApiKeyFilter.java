package com.lethien.user_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * InternalApiKeyFilter — bảo vệ /internal/** endpoints.
 *
 * Chỉ cho phép request có header:
 *   X-Internal-Api-Key: <INTERNAL_API_KEY>
 *
 * Flow:
 *   1. Chỉ apply cho path bắt đầu bằng /internal/
 *   2. Kiểm tra header X-Internal-Api-Key
 *   3. So sánh với giá trị trong application.yml / env
 *   4. Reject 401 nếu sai hoặc thiếu key
 *   5. Pass through nếu đúng key
 *
 * NOTE: Filter này chạy TRƯỚC JwtAuthenticationFilter
 * vì /internal/** không cần JWT — chỉ cần API Key.
 */
@Component
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Chỉ apply cho /internal/** — bỏ qua các path khác
        if (!path.contains(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(API_KEY_HEADER);

        // Missing header
        if (!StringUtils.hasText(providedKey)) {
            log.warn("Internal API call rejected — missing API key header: path={}", path);
            rejectRequest(response, "Missing X-Internal-Api-Key header");
            return;
        }

        // Invalid key
        if (!internalApiKey.equals(providedKey)) {
            log.warn("Internal API call rejected — invalid API key: path={}", path);
            rejectRequest(response, "Invalid API key");
            return;
        }

        log.debug("Internal API call authorized: path={}", path);
        filterChain.doFilter(request, response);
    }

    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":401,\"success\":false,\"message\":\"" + message + "\"}"
        );
    }
}
