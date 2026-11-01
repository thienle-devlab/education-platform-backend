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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * LessonController — quản lý lessons trong section.
 *
 * Endpoints:
 *   GET    /courses/{id}/curriculum              — toàn bộ cấu trúc course (public)
 *   GET    /sections/{id}/lessons                — danh sách lessons (public)
 *   GET    /lessons/{id}                         — detail 1 lesson (public)
 *   POST   /sections/{id}/lessons                — tạo lesson + skillContent (instructor)
 *   PATCH  /lessons/{id}                         — sửa lesson metadata (instructor)
 *   PATCH  /lessons/{id}/skill-content           — upsert skillContent (instructor)
 *   POST   /sections/{id}/lessons/reorder        — đổi thứ tự (instructor)
 *   DELETE /lessons/{id}                         — xóa lesson (instructor)
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
            description = "Returns all sections with their lessons. Public endpoint.")
    @GetMapping("/courses/{courseId}/curriculum")
    public ResponseEntity<ApiResponse<CurriculumResponse>> getCurriculum(
            @PathVariable UUID courseId
    ) {
        log.info("GET /courses/{}/curriculum", courseId);
        CurriculumResponse response = lessonService.getCurriculum(courseId);

        return ResponseEntity.ok(
                ApiResponse.success("Curriculum retrieved successfully", response)
        );
    }

    // ============================================
    // GET /sections/{id}/lessons — PUBLIC
    // ============================================

    @Operation(summary = "Get lessons in a section")
    @GetMapping("/sections/{sectionId}/lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessons(
            @PathVariable UUID sectionId
    ) {
        log.info("GET /sections/{}/lessons", sectionId);
        List<LessonResponse> response = lessonService.getLessonsBySection(sectionId);
        return ResponseEntity.ok(
                ApiResponse.success("Lessons retrieved successfully", response)
        );
    }

    // ============================================
    // GET /lessons/{id} — PUBLIC
    // ============================================

    @Operation(summary = "Get lesson detail by ID")
    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> getLessonById(
            @PathVariable UUID lessonId
    ) {
        log.info("GET /lessons/{}", lessonId);
        LessonResponse response = lessonService.getLessonById(lessonId);
        return ResponseEntity.ok(
                ApiResponse.success("Lesson retrieved successfully", response)
        );
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
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /sections/{}/lessons - accountId={}", sectionId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        LessonResponse response = lessonService.createLesson(
                sectionId, user.getUserId(), request
        );

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
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("PATCH /lessons/{} - accountId={}", lessonId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        LessonResponse response = lessonService.updateLesson(
                lessonId, user.getUserId(), request
        );

        return ResponseEntity.ok(
                ApiResponse.success("Lesson updated successfully", response)
        );
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
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("PATCH /lessons/{}/skill-content - accountId={}", lessonId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        LessonResponse response = lessonService.upsertSkillContent(
                lessonId, user.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Skill content updated successfully", response)
        );
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
            HttpServletRequest httpRequest
    ) {
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
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("DELETE /lessons/{} - accountId={}", lessonId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        lessonService.deleteLesson(lessonId, user.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Lesson deleted successfully")
        );
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private UUID extractAccountId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.getAccountIdFromToken(token);
    }
}
