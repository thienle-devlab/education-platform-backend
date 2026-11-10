package com.lethien.course_service.controller;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.common_lib.dto.PageResponse;
import com.lethien.common_lib.security.JwtService;
import com.lethien.course_service.client.InternalUserResponse;
import com.lethien.course_service.client.UserServiceClient;
import com.lethien.course_service.dto.*;
import com.lethien.course_service.entity.CourseLevel;
import com.lethien.course_service.entity.EnrollmentStatus;
import com.lethien.course_service.entity.SkillType;
import com.lethien.course_service.exception.CourseAccessDeniedException;
import com.lethien.course_service.service.CourseService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * CourseController — REST API for course management.
 *
 * Base path: /courses  (configured via application.yml server.servlet.context-path)
 *
 * Public endpoints (no JWT):
 *   GET  /                     — paginated published catalog with filters
 *   GET  /search               — full-text search
 *   GET  /{id}                 — course detail by ID
 *   GET  /slug/{slug}          — course detail by slug (SEO)
 *
 * Protected endpoints (JWT required):
 *   POST /                     — create course (instructor only)
 *   PATCH /{id}                — update course metadata
 *   POST /{id}/publish         — publish DRAFT course
 *   POST /{id}/archive         — archive course
 *   DELETE /{id}               — delete DRAFT course
 *   GET  /instructor/me        — instructor's own courses
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Course", description = "Course management APIs")
public class CourseController {

    private final CourseService courseService;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    // ============================================
    // PUBLIC — GET /courses (catalog)
    // ============================================

    @Operation(summary = "Get published course catalog",
            description = "Paginated list of published courses with optional filters.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CourseSummaryResponse>>> getPublishedCourses(
            @RequestParam(required = false)SkillType skillType,
            @RequestParam(required = false)CourseLevel level,
            @RequestParam(required = false)BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "totalEnrollments") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
            ) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        PageResponse<CourseSummaryResponse> response = courseService.getPublishedCourses(skillType, level, maxPrice, pageable);

        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", response));
    }

    // ============================================
    // PUBLIC — GET /courses/search
    // ============================================

    @Operation(summary = "Search published courses",
            description = "Full-text search on title and description.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<CourseSummaryResponse>>> searchCourses(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<CourseSummaryResponse> response = courseService.searchCourses(keyword, pageable);

        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", response));
    }

    // ============================================
    // PUBLIC — GET /courses/{id}
    // ============================================

    @Operation(summary = "Get course by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable UUID id) {

        log.info("GET /courses/{}", id);
        CourseResponse response = courseService.getCourseById(id);

        return ResponseEntity.ok(ApiResponse.success("Course retrieved successfully", response));
    }

    // ============================================
    // PUBLIC — GET /courses/slug/{slug}
    // ============================================

    @Operation(summary = "Get course by slug",
            description = "SEO-friendly URL. Only returns PUBLISHED courses.")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseBySlug(@PathVariable String slug) {

        log.info("GET /courses/slug/{}", slug);
        CourseResponse response = courseService.getCourseBySlug(slug);

        return ResponseEntity.ok(ApiResponse.success("Course retrieved successfully", response));
    }

    // ============================================
    // PROTECTED — POST /courses (create)
    // ============================================

    @Operation(summary = "Create a new course", description = "Creates a course in DRAFT status. Instructor only.")
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            HttpServletRequest httpRequest
            ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /courses - accountId={}", accountId);

        // Resolve userId + verify instructor role
        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        // Override instructorId từ server — client không được tự set
        request.setInstructorId(user.getUserId());

        CourseResponse response = courseService.createCourse(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<CourseResponse>builder()
                        .status(201)
                        .success(true)
                        .message("Course created successfully")
                        .data(response)
                        .build()
                );
    }

    // ============================================
    // PROTECTED — PATCH /courses/{id}
    // ============================================

    @Operation(summary = "Update course metadata", description = "Partial update — only non-null fields are applied.")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCourseRequest request,
            HttpServletRequest httpRequest
            ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("PATCH /courses/{} - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        CourseResponse response = courseService.updateCourse(id, user.getUserId(), request);

        return ResponseEntity.ok(ApiResponse.success("Course updated successfully", response));
    }

    // ============================================
    // PROTECTED — POST /courses/{id}/publish
    // ============================================

    @Operation(summary = "Publish a DRAFT course", description = "Changes status from DRAFT to PUBLISHED. Course must have at least 1 lesson.")
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<CourseResponse>> publishCourse(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /courses/{}/publish - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        CourseResponse response = courseService.publishCourse(id, user.getUserId());

        return ResponseEntity.ok(ApiResponse.success("Course published successfully", response));
    }

    // ============================================
    // PROTECTED — POST /courses/{id}/archive
    // ============================================

    @Operation(summary = "Archive a course", description = "Changes status to ARCHIVED. Existing enrollments are not affected.")
    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<CourseResponse>> archiveCourse(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /courses/{}/archive - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        CourseResponse response = courseService.archiveCourse(id, user.getUserId());

        return ResponseEntity.ok(ApiResponse.success("Course archived successfully", response));
    }

    // ============================================
    // PROTECTED — DELETE /courses/{id}
    // ============================================

    @Operation(summary = "Delete a DRAFT course", description = "Only DRAFT courses can be deleted. PUBLISHED must be archived first.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("DELETE /courses/{} - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        courseService.deleteCourse(id, user.getUserId());

        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully"));
    }

    // ============================================
    // PROTECTED — GET /courses/instructor/me
    // ============================================

    @Operation(summary = "Get instructor's own courses",
            description = "Returns all courses (DRAFT, PUBLISHED, ARCHIVED) for the authenticated instructor.")
    @GetMapping("/instructor/me")
    public ResponseEntity<ApiResponse<PageResponse<CourseSummaryResponse>>> getMyCourses(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest
    ) {
        // TODO [STEP-5 Inter-service]: accountId từ JWT cần được map sang userId (instructorId)
        // thông qua UserServiceClient.getUserIdByAccountId(accountId).
        // Sau đó truyền userId đó vào courseService.getCoursesByInstructor() thay vì
        // nhận instructorId từ @RequestParam như các endpoint khác.
        // Khi đó endpoint /me mới thực sự đúng nghĩa — identity hoàn toàn từ JWT.
        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /courses/instructor/me - accountId={}", accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        // Tạm thời dùng accountId như instructorId cho đến khi có UserServiceClient
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<CourseSummaryResponse> response =
                courseService.getCoursesByInstructor(user.getUserId(), pageable);

        return ResponseEntity.ok(ApiResponse.success("Instructor courses retrieved", response));
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private UUID extractAccountId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7); // strip "Bearer "
        return jwtService.getAccountIdFromToken(token);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }
}
