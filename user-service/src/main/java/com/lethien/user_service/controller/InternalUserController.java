package com.lethien.user_service.controller;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.user_service.dto.InternalUserResponse;
import com.lethien.user_service.entity.User;
import com.lethien.user_service.exception.UserNotFoundException;
import com.lethien.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * InternalUserController — inter-service endpoints cho User Service.
 *
 * Base path: /internal/users
 *
 * Bảo mật: InternalApiKeyFilter — chỉ cho phép request có
 * header X-Internal-Api-Key đúng giá trị. Không cần JWT.
 *
 * Hiện tại được gọi bởi:
 *   - Course Service (UserServiceClient)
 *     → GET /internal/users/account/{accountId}
 *       để map accountId → userId + roles
 *
 * KHÔNG expose ra public API gateway —
 * chỉ accessible trong Docker internal network.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserProfileRepository userProfileRepository;

    // ============================================
    // GET /internal/account/{accountId}
    // ============================================

    /**
     * Map accountId (từ Auth Service JWT) → userId + roles.
     *
     * Called by: Course Service — UserServiceClient
     * Use cases:
     *   - Verify instructor ownership khi tạo/sửa/xóa course
     *   - Verify role trước khi thực hiện action
     *   - Lấy userId thật để lưu vào enrollments, submissions
     *
     * @param accountId accountId từ JWT claim
     */

    @GetMapping("/account/{accountId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<InternalUserResponse>> getUserByAccountId(@PathVariable UUID accountId) {

        log.info("Internal: getUserByAccountId: accountId={}", accountId);

        User user = userProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new UserNotFoundException("User not found for accountId: " + accountId));

        InternalUserResponse response = InternalUserResponse.from(user);

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", response)
        );
    }

    // ============================================
    // GET /internal/{userId}
    // ============================================

    /**
     * Lấy thông tin user theo userId (PK trong user_db).
     *
     * Called by: Course Service — khi cần lấy thông tin instructor
     * để hiển thị trong CourseResponse.
     *
     * @param userId user PK trong user_db
     */
    @GetMapping("/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<InternalUserResponse>> getUserById(@PathVariable UUID userId) {

        log.info("Internal: getUserById: userId={}", userId);

        User user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found: " + userId
                ));

        InternalUserResponse response = InternalUserResponse.from(user);

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", response)
        );
    }

}
