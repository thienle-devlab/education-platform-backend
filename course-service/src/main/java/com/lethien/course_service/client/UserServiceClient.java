package com.lethien.course_service.client;

import com.lethien.course_service.exception.UserNotFoundException;
import com.lethien.course_service.exception.UserServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * UserServiceClient — gọi User Service qua HTTP (WebClient).
 *
 * Dùng Hướng B: X-Internal-Api-Key header cho inter-service auth.
 * Không dùng JWT vì đây là server-to-server call.
 *
 * Base URL (đã gồm context-path của User Service):
 *   - Local : http://localhost:8082/api/users
 *   - Docker: http://user-service:8082/api/users
 * Được config qua application.yml → services.user-service.url
 *
 * Sử dụng WebClient synchronous (.block()) vì Course Service
 * đang dùng Spring WebMVC (blocking stack).
 */
@Component
@Slf4j
public class UserServiceClient {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public UserServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${services.user-service.url}") String userServiceUrl,
            @Value("${services.user-service.context-path:}") String contextPath,
            @Value("${internal.api-key}") String internalApiKey
    ) {
        String baseUrl = userServiceUrl + contextPath;

        log.info("User Service baseUrl = {}", baseUrl);

        this.webClient = webClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(API_KEY_HEADER, internalApiKey)
                .build();
    }

    // ============================================
    // GET /internal/users/account/{accountId}
    // ============================================

    /**
     * Map accountId từ JWT → userId + roles trong User Service.
     *
     * Dùng ở: CourseController, EnrollmentController, ExerciseController
     * để resolve identity trước khi xử lý business logic.
     *
     * GET /internal/users/account/{accountId}
     *
     * @param accountId accountId từ JWT claim
     * @return InternalUserResponse chứa userId, roles, isInstructor
     * @throws UserNotFoundException nếu User Service trả 404
     * @throws UserServiceException  nếu User Service lỗi, timeout hoặc không kết nối được
     */
    public InternalUserResponse getUserByAccountId(UUID accountId) {
        log.debug("Calling User Service: getUserByAccountId={}", accountId);

        return fetchUser(
                "/internal/users/account/{id}",
                accountId,
                "accountId: " + accountId
        );
    }

    // ============================================
    // GET /internal/users/{userId}
    // ============================================

    /**
     * Lấy thông tin user theo userId.
     *
     * Dùng ở: CourseService để lấy thông tin instructor
     * hiển thị trong CourseResponse.
     *
     * GET /internal/users/{userId}
     *
     * @param userId user PK trong user_db
     * @throws UserNotFoundException nếu User Service trả 404
     * @throws UserServiceException  nếu User Service lỗi, timeout hoặc không kết nối được
     */
    public InternalUserResponse getUserById(UUID userId) {
        log.debug("Calling User Service: getUserById={}", userId);

        return fetchUser(
                "/internal/users/{id}",
                userId,
                "userId: " + userId
        );
    }

    // ============================================
    // PRIVATE HELPER
    // ============================================

    /**
     * Gọi User Service và parse kết quả. Dùng chung cho mọi endpoint lấy user
     * để hành vi xử lý lỗi luôn nhất quán.
     *
     * @param path đường dẫn SAU context-path, bắt đầu bằng "/" (không chứa host hay /api/users)
     * @param id   giá trị thay cho placeholder {id}
     * @param desc mô tả ngắn để đưa vào log / message lỗi
     */
    private InternalUserResponse fetchUser(String path, UUID id, String desc) {
        log.debug("Calling User Service: GET {} (id={})", path, id);

        try {
            Map<String, Object> response = webClient.get()
                    .uri(path, id)
                    .retrieve()
                    // Chỉ 404 mới là "user không tồn tại"
                    .onStatus(status -> status.value() == 404, r ->
                            r.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new UserNotFoundException(
                                            "User not found for " + desc
                                    )))
                    // Các lỗi còn lại (400, 401, 403, 5xx...) báo kèm status và path
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new UserServiceException(
                                            "User Service returned " + r.statusCode().value()
                                                    + " for " + path + ": " + body
                                    )))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(TIMEOUT);

            if (response == null || response.get("data") == null) {
                throw new UserServiceException(
                        "Empty response from User Service for " + desc
                );
            }

            // Parse data field từ ApiResponse wrapper
            return parseUserResponse(response.get("data"));
        } catch (UserNotFoundException e) {
            throw e;
        } catch (UserServiceException e) {
            throw e;
        } catch (Exception e) {
            // Chỉ còn lỗi kết nối, timeout, parse JSON...
            log.error("Failed to reach User Service: {}, error={}", desc, e.getMessage());
            throw new UserServiceException("Failed to reach User Service: " + e.getMessage());
        }
    }

    /**
     * Parse Map<String, Object> từ ApiResponse.data → InternalUserResponse.
     */
    @SuppressWarnings("unchecked")
    private InternalUserResponse parseUserResponse(Object data) {
        if (!(data instanceof Map)) {
            throw new UserServiceException("Unexpected response format from User Service");
        }

        Map<String, Object> map = (Map<String, Object>) data;

        return InternalUserResponse.builder()
                .userId(parseUUID(map.get("userId")))
                .accountId(parseUUID(map.get("accountId")))
                .fullName((String) map.get("fullName"))
                .email((String) map.get("email"))
                .avatarUrl((String) map.get("avatarUrl"))
                .roles(map.get("roles") instanceof java.util.List
                        ? new java.util.HashSet<>((java.util.List<String>) map.get("roles"))
                        : java.util.Set.of())
                .isInstructor(Boolean.TRUE.equals(map.get("instructor")))
                .isAdmin(Boolean.TRUE.equals(map.get("admin")))
                .build();
    }

    private UUID parseUUID(Object value) {
        if (value == null) return null;
        return UUID.fromString(value.toString());
    }
}
