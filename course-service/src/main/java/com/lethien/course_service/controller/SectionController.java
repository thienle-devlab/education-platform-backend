package com.lethien.course_service.controller;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.common_lib.security.JwtService;
import com.lethien.course_service.client.InternalUserResponse;
import com.lethien.course_service.client.UserServiceClient;
import com.lethien.course_service.dto.CreateSectionRequest;
import com.lethien.course_service.dto.ReorderSectionsRequest;
import com.lethien.course_service.dto.SectionResponse;
import com.lethien.course_service.dto.UpdateSectionRequest;
import com.lethien.course_service.service.SectionService;
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
 * SectionController — quản lý sections trong course.
 *
 * Base path: /courses/{courseId}/sections
 *
 * Endpoints:
 *   GET    /courses/{id}/sections           — danh sách sections (public)
 *   POST   /courses/{id}/sections           — tạo section (instructor only)
 *   PATCH  /sections/{id}                   — sửa section
 *   DELETE /sections/{id}                   — xóa section + cascade
 *   POST   /courses/{id}/sections/reorder   — đổi thứ tự sections
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Section", description = "Course section management APIs")
public class SectionController {

    private final SectionService sectionService;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    // ============================================
    // GET /courses/{id}/sections — PUBLIC
    // ============================================

    @Operation(summary = "Get all sections of a course")
    @GetMapping("/courses/{courseId}/sections")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getSections(
            @PathVariable UUID courseId
    ) {
        log.info("GET /courses/{}/sections", courseId);

        List<SectionResponse> response = sectionService.getSectionsByCourse(courseId);

        return ResponseEntity.ok(
                ApiResponse.success("Sections retrieved successfully", response)
        );
    }

    // ============================================
    // POST /courses/{id}/sections — INSTRUCTOR
    // ============================================

    @Operation(summary = "Create a new section",
            description = "Instructor only. Section is appended at the end.")
    @PostMapping("/courses/{courseId}/sections")
    public ResponseEntity<ApiResponse<SectionResponse>> createSection(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateSectionRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /courses/{}/sections - accountId={}", courseId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        SectionResponse response = sectionService.createSection(
                courseId, user.getUserId(), request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<SectionResponse>builder()
                        .status(201).success(true)
                        .message("Section created successfully")
                        .data(response).build());
    }

    // ============================================
    // PATCH /sections/{id} — INSTRUCTOR
    // ============================================

    @Operation(summary = "Update section title/description")
    @PatchMapping("/sections/{sectionId}")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(
            @PathVariable UUID sectionId,
            @Valid @RequestBody UpdateSectionRequest request,
            HttpServletRequest httpRequest
            ) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("PATCH /sections/{} - accountId={}", sectionId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        SectionResponse response = sectionService.updateSection(
                sectionId, user.getUserId(), request
        );

        return ResponseEntity.ok(
                ApiResponse.success("Section updated successfully", response)
        );
    }

    // ============================================
    // POST /courses/{id}/sections/reorder — INSTRUCTOR
    // ============================================

    @Operation(summary = "Reorder sections",
            description = "Pass all sectionIds in the desired order.")
    @PostMapping("/courses/{courseId}/sections/reorder")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> reorderSections(
            @PathVariable UUID courseId,
            @Valid @RequestBody ReorderSectionsRequest request,
            HttpServletRequest httpRequest
            ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /courses/{}/sections/reorder - accountId={}", courseId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        List<SectionResponse> response = sectionService.reorderSections(
                courseId, user.getUserId(), request
        );

        return ResponseEntity.ok(
                ApiResponse.success("Sections reordered successfully", response)
        );
    }

    // ============================================
    // DELETE /sections/{id} — INSTRUCTOR
    // ============================================

    @Operation(summary = "Delete a section",
            description = "Cascades to all lessons and exercises. " +
                    "Blocked if course has active enrollments.")
    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable UUID sectionId,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("DELETE /sections/{} - accountId={}", sectionId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        sectionService.deleteSection(sectionId, user.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Section deleted successfully")
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
