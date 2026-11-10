package com.lethien.course_service.controller;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.common_lib.dto.PageResponse;
import com.lethien.common_lib.security.JwtService;
import com.lethien.course_service.client.InternalUserResponse;
import com.lethien.course_service.client.UserServiceClient;
import com.lethien.course_service.dto.CreateReviewRequest;
import com.lethien.course_service.dto.ReviewResponse;
import com.lethien.course_service.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * ReviewController — REST API for course reviews.
 *
 * Endpoints:
 *   GET    /courses/{id}/reviews      — danh sách review (public)
 *   POST   /courses/{id}/reviews      — tạo review (enrolled user)
 *   GET    /courses/{id}/reviews/me   — review của tôi
 *   DELETE /courses/{id}/reviews/me   — xóa review của tôi
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review", description = "Course review APIs")
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    // ============================================
    // GET /courses/{id}/reviews — PUBLIC
    // ============================================

    @Operation(summary = "Get course reviews",
            description = "Paginated list of verified purchase reviews. Public endpoint.")
    @GetMapping("/courses/{id}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getCourseReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ) {
        log.info("GET /courses/{}/reviews", id);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        PageResponse<ReviewResponse> response =
                reviewService.getCourseReviews(id, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Reviews retrieved successfully", response)
        );
    }

    // ============================================
    // POST /courses/{id}/reviews — PROTECTED
    // ============================================

    @Operation(summary = "Create a review",
            description = "User must be enrolled (ACTIVE or COMPLETED). " +
                    "Each user can only review once per course.")
    @PostMapping("/courses/{id}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable UUID id,
            @Valid @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest
            ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /courses/{}/reviews - accountId={}", id, accountId);

        // Map accountId → userId thật từ User Service
        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        ReviewResponse response = reviewService.createReview(id, user.getUserId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<ReviewResponse>builder()
                        .status(201)
                        .success(true)
                        .message("Review created successfully")
                        .data(response)
                        .build());
    }

    // ============================================
    // GET /courses/{id}/reviews/me — PROTECTED
    // ============================================

    @Operation(summary = "Get my review for a course",
              description = "Returns the authenticated user's review for this course.")
    @GetMapping("/courses/{id}/reviews/me")
    public ResponseEntity<ApiResponse<ReviewResponse>> getMyReview(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /courses/{}/reviews/me - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        ReviewResponse response = reviewService.getMyReview(id, user.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Review retrieved successfully", response)
        );
    }

    // ============================================
    // DELETE /courses/{id}/reviews/me — PROTECTED
    // ============================================

    @Operation(summary = "Delete my review",
            description = "Delete the authenticated user's review for this course.")
    @DeleteMapping("/courses/{id}/reviews/me")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("DELETE /courses/{}/reviews/me - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        reviewService.deleteReview(id, user.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Review deleted successfully")
        );
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private UUID extractAccountId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        return jwtService.getAccountIdFromToken(token);
    }
}
