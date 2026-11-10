package com.lethien.course_service.controller;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.common_lib.dto.PageResponse;
import com.lethien.common_lib.security.JwtService;
import com.lethien.course_service.client.InternalUserResponse;
import com.lethien.course_service.client.UserServiceClient;
import com.lethien.course_service.dto.EnrollmentResponse;
import com.lethien.course_service.dto.LessonProgressResponse;
import com.lethien.course_service.dto.UpdateLessonProgressRequest;
import com.lethien.course_service.entity.EnrollmentStatus;
import com.lethien.course_service.service.EnrollmentService;
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

import java.util.List;
import java.util.UUID;

/**
 * EnrollmentController — REST API for course enrollment management.
 *
 * Inter-service: accountId từ JWT → userId thật qua UserServiceClient.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enrollment", description = "Course enrollment and progress tracking APIs")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    // ============================================
    // POST /courses/{courseId}/enroll
    // ============================================

    @Operation(summary = "Enroll into a course",
            description = "Enroll the authenticated user into a PUBLISHED course. " +
                    "Reactivates existing enrollment if previously CANCELLED/EXPIRED.")
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @PathVariable UUID courseId,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /courses/{}/enroll - accountId={}", courseId, accountId);

        // Map accountId → userId thật từ User Service
        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        EnrollmentResponse response  = enrollmentService.enroll(user.getUserId(), courseId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<EnrollmentResponse>builder()
                        .status(201)
                        .success(true)
                        .message("Enrolled successfully")
                        .data(response)
                        .build());
    }

    // ============================================
    // GET /enrollments/me
    // ============================================

    @Operation(summary = "Get my enrollments",
            description = "Paginated list of all enrollments for the authenticated user.")
    @GetMapping("/enrollments/me")
    public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> getMyEnrollments(
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /enrollments/me - accountId={}, status={}", accountId, status);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("enrolledAt").descending());

        PageResponse<EnrollmentResponse> response = status != null
                ? enrollmentService.getMyEnrollmentsByStatus(user.getUserId(), status, pageable)
                : enrollmentService.getMyEnrollments(user.getUserId(), pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Enrollments retrieved successfully", response)
        );
    }

    // ============================================
    // GET /enrollments/{id}
    // ============================================

    @Operation(summary = "Get enrollment detail by ID")
    @GetMapping("/enrollments/{id}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> getEnrollmentById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /enrollments/{} - accountId={}", id, accountId);

        EnrollmentResponse response = enrollmentService.getEnrollmentById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Enrollment retrieved successfully", response)
        );
    }

    // ============================================
    // GET /enrollments/{id}/progress
    // ============================================

    @Operation(summary = "Get lesson progress for an enrollment")
    @GetMapping("/enrollments/{id}/progress")
    public ResponseEntity<ApiResponse<List<LessonProgressResponse>>> getLessonProgress(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /enrollments/{}/progress - accountId={}", id, accountId);

        List<LessonProgressResponse> response = enrollmentService.getLessonProgress(id);

        return ResponseEntity.ok(
                ApiResponse.success("Lesson progress retrieved successfully", response)
        );
    }

    // ============================================
    // POST /enrollments/{id}/lessons/{lessonId}/progress
    // ============================================

    @Operation(summary = "Update lesson progress",
            description = "Mark a lesson as IN_PROGRESS or COMPLETED.")
    @PostMapping("/enrollments/{id}/lessons/{lessonId}/progress")
    public ResponseEntity<ApiResponse<LessonProgressResponse>> updateLessonProgress(
            @PathVariable UUID id,
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateLessonProgressRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /enrollments/{}/lessons/{}/progress - accountId={}",
                id, lessonId, accountId);

        // Set userId thật vào request
        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        request.setUserId(user.getUserId());

        LessonProgressResponse response = enrollmentService.updateLessonProgress(id, lessonId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Lesson progress updated successfully", response)
        );
    }

    // ============================================
    // POST /enrollments/{id}/cancel
    // ============================================

    @Operation(summary = "Cancel an enrollment")
    @PostMapping("/enrollments/{id}/cancel")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> cancelEnrollment(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /enrollments/{}/cancel - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        EnrollmentResponse response =
                enrollmentService.cancelEnrollment(id, user.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Enrollment cancelled successfully", response)
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
