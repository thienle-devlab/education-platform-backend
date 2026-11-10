package com.lethien.auth_service.client;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * UserServiceClient — Auth Service gọi User Service để lấy roles khi login.
 *
 * Chỉ cần 1 method: getUserByAccountId() để lấy roles.
 * Nếu User Service down → trả về empty roles, không fail login.
 */
@Component
@Slf4j
public class UserServiceClient {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Duration TIMEOUT = Duration.ofSeconds(3); // Ngắn hơn Course Service — login cần nhanh

    private final WebClient webClient;

    public UserServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.user-service.url}") String userServiceUrl,
            @Value("${services.user-service.context-path:}") String contextPath,
            @Value("${internal.api-key}") String internalApiKey) {

        String baseUrl = userServiceUrl + contextPath;
        log.info("Auth Service → User Service baseUrl = {}", baseUrl);

        this.webClient = webClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(API_KEY_HEADER, internalApiKey)
                .build();
    }

    /**
     * Lấy thông tin user theo accountId — chủ yếu để lấy roles khi login.
     *
     * @param accountId accountId từ auth_db
     * @return UserInfo chứa roles
     */
    public UserInfo getUserByAccountId(UUID accountId) {
        log.debug("Fetching user roles for accountId={}", accountId);

        try {
            Map<String, Object> response = webClient.get()
                    .uri("/internal/users/account/{id}", accountId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r ->
                            r.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new RuntimeException(
                                            "User not found for accountId: " + accountId)))
                    .onStatus(HttpStatusCode::is5xxServerError, r ->
                            r.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new RuntimeException(
                                            "User Service error: " + body)))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(TIMEOUT);

            if (response == null || response.get("data") == null) {
                return UserInfo.empty();
            }

            return parseUserInfo(response.get("data"));

        } catch (Exception e) {
            log.warn("Failed to fetch user info from User Service: accountId={}, error={}",
                    accountId, e.getMessage());
            return UserInfo.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private UserInfo parseUserInfo(Object data) {
        if (!(data instanceof Map)) return UserInfo.empty();

        Map<String, Object> map = (Map<String, Object>) data;

        Set<String> roles = Set.of();
        if (map.get("roles") instanceof List<?> list) {
            roles = new HashSet<>((List<String>) list);
        }

        return UserInfo.builder()
                .userId(parseUUID(map.get("userId")))
                .roles(roles)
                .isInstructor(Boolean.TRUE.equals(map.get("instructor")))
                .isAdmin(Boolean.TRUE.equals(map.get("admin")))
                .build();
    }

    private UUID parseUUID(Object value) {
        if (value == null) return null;
        return UUID.fromString(value.toString());
    }

    // ============================================
    // INNER DTO
    // ============================================

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID userId;
        private Set<String> roles;
        private boolean isInstructor;
        private boolean isAdmin;

        public static UserInfo empty() {
            return UserInfo.builder()
                    .roles(Set.of())
                    .isInstructor(false)
                    .isAdmin(false)
                    .build();
        }
    }
}