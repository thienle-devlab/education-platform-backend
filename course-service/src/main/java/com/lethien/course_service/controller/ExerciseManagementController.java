package com.lethien.course_service.controller;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.common_lib.security.JwtService;
import com.lethien.course_service.client.InternalUserResponse;
import com.lethien.course_service.client.UserServiceClient;
import com.lethien.course_service.dto.CreateExerciseRequest;
import com.lethien.course_service.dto.ExerciseResponse;
import com.lethien.course_service.dto.ReorderExercisesRequest;
import com.lethien.course_service.dto.UpdateExerciseRequest;
import com.lethien.course_service.service.ExerciseManagementService;
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
 * ExerciseManagementController — instructor quản lý exercises trong lesson.
 *
 * Tách khỏi ExerciseController (học viên nộp bài) để hai phần không lẫn quyền.
 *
 * Endpoints:
 *   GET    /lessons/{id}/exercises          — owner: đầy đủ; học viên: ẩn đáp án (cần JWT)
 *   POST   /lessons/{id}/exercises          — tạo exercise (INSTRUCTOR owner)
 *   POST   /lessons/{id}/exercises/reorder  — đổi thứ tự (INSTRUCTOR owner)
 *   PATCH  /exercises/{id}                  — sửa exercise (INSTRUCTOR owner)
 *   DELETE /exercises/{id}                  — xóa exercise (INSTRUCTOR owner)
 *
 * Phân quyền 2 lớp: SecurityConfig kiểm tra role INSTRUCTOR,
 * ExerciseManagementService kiểm tra đúng chủ sở hữu course.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Exercise Management", description = "Instructor APIs to manage exercises inside a lesson")
public class ExerciseManagementController {

    private final ExerciseManagementService exerciseManagementService;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    // ============================================
    // GET /lessons/{id}/exercises — AUTHENTICATED
    // ============================================

    @Operation(summary = "List exercises of a lesson",
            description = "Course owner sees correctAnswer and explanation. Learners (enrolled, " +
                    "or preview lesson of a published course) do not.")
    @GetMapping("/lessons/{lessonId}/exercises")
    public ResponseEntity<ApiResponse<List<ExerciseResponse>>> getExercises(
            @PathVariable UUID lessonId,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /lessons/{}/exercises - accountId={}", lessonId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        List<ExerciseResponse> response = exerciseManagementService.getExercises(lessonId, user.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Exercises retrieved successfully", response)
        );
    }

    // ============================================
    // POST /lessons/{id}/exercises — INSTRUCTOR
    // ============================================

    @Operation(summary = "Create an exercise",
            description = "Lesson must be of type EXERCISE. options/correctAnswer are validated per exerciseType.")
    @PostMapping("/lessons/{lessonId}/exercises")
    public ResponseEntity<ApiResponse<ExerciseResponse>> createExercise(
            @PathVariable UUID lessonId,
            @Valid @RequestBody CreateExerciseRequest request,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /lessons/{}/exercises - accountId={}", lessonId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        ExerciseResponse response =
                exerciseManagementService.createExercise(lessonId, user.getUserId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<ExerciseResponse>builder()
                        .status(201).success(true)
                        .message("Exercise created successfully")
                        .data(response).build());
    }

    // ============================================
    // POST /lessons/{id}/exercises/reorder — INSTRUCTOR
    // ============================================

    @Operation(summary = "Reorder exercises in a lesson",
            description = "Pass all exerciseIds of the lesson in the desired order.")
    @PostMapping("/lessons/{lessonId}/exercises/reorder")
    public ResponseEntity<ApiResponse<List<ExerciseResponse>>> reorderExercises(
            @PathVariable UUID lessonId,
            @Valid @RequestBody ReorderExercisesRequest request,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /lessons/{}/exercises/reorder - accountId={}", lessonId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        List<ExerciseResponse> response =
                exerciseManagementService.reorderExercises(lessonId, user.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Exercises reordered successfully", response));
    }

    // ============================================
    // PATCH /exercises/{id} — INSTRUCTOR
    // ============================================

    @Operation(summary = "Update an exercise",
            description = "Partial update. exerciseType, options, correctAnswer and points are " +
                    "locked once the exercise has attempts.")
    @PatchMapping("/exercises/{exerciseId}")
    public ResponseEntity<ApiResponse<ExerciseResponse>> updateExercise(
            @PathVariable UUID exerciseId,
            @Valid @RequestBody UpdateExerciseRequest request,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("PATCH /exercises/{} - accountId={}", exerciseId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        ExerciseResponse response =
                exerciseManagementService.updateExercise(exerciseId, user.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Exercise updated successfully", response));
    }

    // ============================================
    // DELETE /exercises/{id} — INSTRUCTOR
    // ============================================

    @Operation(summary = "Delete an exercise",
            description = "Blocked when the course is not DRAFT and has enrollments.")
    @DeleteMapping("/exercises/{exerciseId}")
    public ResponseEntity<ApiResponse<Void>> deleteExercise(
            @PathVariable UUID exerciseId,
            HttpServletRequest httpRequest) {

        UUID accountId = extractAccountId(httpRequest);
        log.info("DELETE /exercises/{} - accountId={}", exerciseId, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        exerciseManagementService.deleteExercise(exerciseId, user.getUserId());

        return ResponseEntity.ok(ApiResponse.success("Exercise deleted successfully"));
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private UUID extractAccountId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtService.getAccountIdFromToken(token);
    }
}
