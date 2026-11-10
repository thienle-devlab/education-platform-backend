package com.lethien.course_service.controller;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.common_lib.security.JwtService;
import com.lethien.course_service.client.InternalUserResponse;
import com.lethien.course_service.client.UserServiceClient;
import com.lethien.course_service.dto.*;
import com.lethien.course_service.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * LessonController — quản lý lessons trong section.
 *
 * Endpoints đọc (công khai, JWT là tùy chọn):
 *   GET /courses/{id}/curriculum   — cấu trúc course
 *   GET /sections/{id}/lessons     — lessons của section
 *   GET /lessons/{id}              — chi tiết 1 lesson
 *   Có JWT hợp lệ → OWNER / ENROLLED thấy đầy đủ nội dung.
 *   Khách hoặc chưa enroll → chỉ thấy cấu trúc và nội dung của lesson preview.
 *
 * Endpoints ghi (INSTRUCTOR owner):
 *   POST   /sections/{id}/lessons
 *   PATCH  /lessons/{id}
 *   PATCH  /lessons/{id}/skill-content
 *   POST   /sections/{id}/lessons/reorder
 *   DELETE /lessons/{id}
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lesson", description = "Course lesson management APIs")
public class LessonController {

    private final LessonService lessonService;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    // ============================================
    // GET /courses/{id}/curriculum — PUBLIC
    // ============================================

    @Operation(summary = "Get full course curriculum",
            description = "Public. Lesson content (contentUrl, skillContent) is only included for " +
                    "preview lessons, or for the course owner / enrolled learners (send a JWT).")
    @GetMapping("/courses/{courseId}/curriculum")
    public ResponseEntity<ApiResponse<CurriculumResponse>> getCurriculum(
            @PathVariable UUID courseId) {

        log.info("GET /courses/{}/curriculum", courseId);
        CurriculumResponse response =
                lessonService.getCurriculum(courseId, resolveOptionalViewerUserId());
        return ResponseEntity.ok(
                ApiResponse.success("Curriculum retrieved successfully", response));
    }

    // ============================================
    // GET /sections/{id}/lessons — PUBLIC (JWT tùy chọn)
    // ============================================

    @Operation(summary = "Get lessons in a section",
            description = "Same visibility rules as the curriculum endpoint.")
    @GetMapping("/sections/{sectionId}/lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessons(
            @PathVariable UUID sectionId) {

        log.info("GET /sections/{}/lessons", sectionId);
        List<LessonResponse> response =
                lessonService.getLessonsBySection(sectionId, resolveOptionalViewerUserId());
        return ResponseEntity.ok(
                ApiResponse.success("Lessons retrieved successfully", response));
    }

    // ============================================
    // GET /lessons/{id} — PUBLIC (JWT tùy chọn)
    // ============================================

    @Operation(summary = "Get lesson detail by ID",
            description = "Locked lessons return contentLocked=true without contentUrl/skillContent.")
    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> getLessonById(
            @PathVariable UUID lessonId) {

        log.info("GET /lessons/{}", lessonId);
        LessonResponse response =
                lessonService.getLessonById(lessonId, resolveOptionalViewerUserId());
        return ResponseEntity.ok(
                ApiResponse.success("Lesson retrieved successfully", response));
    }

    // ============================================
    // POST /sections/{id}/lessons — INSTRUCTOR
    // ============================================

    @Operation(summary = "Create a new lesson",
            description = "Optionally include skillContent to create both in one request.")
    @PostMapping("/sections/{sectionId}/lessons")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable UUID sectionId,
            @Valid @RequestBody CreateLessonRequest request,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /sections/{}/lessons - accountId={}", sectionId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        LessonResponse response = lessonService.createLesson(
                sectionId, user.getUserId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<LessonResponse>builder()
                        .status(201).success(true)
                        .message("Lesson created successfully")
                        .data(response).build());
    }

    // ============================================
    // PATCH /lessons/{id} — INSTRUCTOR
    // ============================================

    @Operation(summary = "Update lesson metadata",
            description = "Partial update. lessonType cannot be changed after creation.")
    @PatchMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID lessonId,
            @Valid @RequestBody UpdateLessonRequest request,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("PATCH /lessons/{} - accountId={}", lessonId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        LessonResponse response = lessonService.updateLesson(
                lessonId, user.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Lesson updated successfully", response));
    }

    // ============================================
    // PATCH /lessons/{id}/skill-content — INSTRUCTOR
    // ============================================

    @Operation(summary = "Create or update skill content for a lesson",
            description = "Use this when skill content was not provided during lesson creation.")
    @PatchMapping("/lessons/{lessonId}/skill-content")
    public ResponseEntity<ApiResponse<LessonResponse>> upsertSkillContent(
            @PathVariable UUID lessonId,
            @Valid @RequestBody SkillContentRequest request,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("PATCH /lessons/{}/skill-content - accountId={}", lessonId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        LessonResponse response = lessonService.upsertSkillContent(
                lessonId, user.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Skill content updated successfully", response));
    }

    // ============================================
    // POST /sections/{id}/lessons/reorder — INSTRUCTOR
    // ============================================

    @Operation(summary = "Reorder lessons in a section",
            description = "Pass all lessonIds of the section in desired order.")
    @PostMapping("/sections/{sectionId}/lessons/reorder")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> reorderLessons(
            @PathVariable UUID sectionId,
            @Valid @RequestBody ReorderLessonsRequest request,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /sections/{}/lessons/reorder - accountId={}", sectionId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        List<LessonResponse> response = lessonService.reorderLessons(
                sectionId, user.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Lessons reordered successfully", response));
    }

    // ============================================
    // DELETE /lessons/{id} — INSTRUCTOR
    // ============================================

    @Operation(summary = "Delete a lesson",
            description = "Cascades to exercises and submissions. " +
                    "Blocked if course has active enrollments.")
    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable UUID lessonId,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("DELETE /lessons/{} - accountId={}", lessonId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        lessonService.deleteLesson(lessonId, user.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Lesson deleted successfully"));
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private UUID extractAccountId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.getAccountIdFromToken(token);
    }

    /**
     * Xác định người xem cho các endpoint công khai.
     *
     * JwtAuthenticationFilter vẫn chạy ở endpoint permitAll: token hợp lệ thì đặt
     * accountId (UUID) làm principal; không có token hoặc token sai/hết hạn thì
     * principal là "anonymousUser" → coi như khách (trả null).
     *
     * Nếu không tra được userId (User Service lỗi, chưa có profile...) cũng coi như khách,
     * không làm hỏng một endpoint công khai chỉ vì bước cá nhân hóa này.
     */
    private UUID resolveOptionalViewerUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID accountId)) {
            return null;
        }
        try {
            return userServiceClient.getUserByAccountId(accountId).getUserId();
        } catch (RuntimeException e) {
            log.warn("Could not resolve viewer for accountId={}, treating as guest: {}",
                    accountId, e.getMessage());
            return null;
        }
    }
}
