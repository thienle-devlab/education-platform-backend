package com.lethien.course_service.controller;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.common_lib.security.JwtService;
import com.lethien.course_service.client.InternalUserResponse;
import com.lethien.course_service.client.UserServiceClient;
import com.lethien.course_service.dto.*;
import com.lethien.course_service.service.ExerciseService;
import com.lethien.course_service.service.SubmissionService;
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
 * ExerciseController — REST API for exercise attempts and submissions.
 * Base path: /exercises
 *
 * Inter-service: accountId từ JWT → userId thật qua UserServiceClient.
 */
@RestController
@RequestMapping("/exercises")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Exercise", description = "Exercise attempt and submission APIs")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final SubmissionService submissionService;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    // ============================================
    // POST /exercises/{id}/attempts
    // ============================================

    @Operation(summary = "Submit an auto-graded exercise attempt",
            description = "Supports: MCQ, FILL_IN_BLANK, TRUE_FALSE, MATCHING, " +
                    "ORDERING, SHORT_ANSWER, DICTATION.")
    @PostMapping("/{id}/attempts")
    public ResponseEntity<ApiResponse<ExerciseAttemptResponse>> submitAttempt(
            @PathVariable UUID id,
            @Valid @RequestBody ExerciseAttemptRequest request,
            HttpServletRequest httpRequest
            ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /exercises/{}/attempts - accountId={}", id, accountId);

        // Map accountId → userId thật
        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        request.setUserId(user.getUserId());

        ExerciseAttemptResponse response = exerciseService.submitAttempt(id, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<ExerciseAttemptResponse>builder()
                        .status(201)
                        .success(true)
                        .message("Attempt submitted successfully")
                        .data(response)
                        .build());
    }

    // ============================================
    // GET /exercises/{id}/attempts/me
    // ============================================

    @Operation(summary = "Get my attempt history for an exercise")
    @GetMapping("/{id}/attempts/me")
    public ResponseEntity<ApiResponse<List<ExerciseAttemptResponse>>> getMyAttempts(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /exercises/{}/attempts/me - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);

        List<ExerciseAttemptResponse> response = exerciseService.getMyAttempts(id, user.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Attempts retrieved successfully", response)
        );
    }

    // ============================================
    // POST /exercises/{id}/speaking
    // ============================================

    @Operation(summary = "Submit a speaking recording",
            description = "Returns immediately with status=PENDING. Poll GET for result.")
    @PostMapping("/{id}/speaking")
    public ResponseEntity<ApiResponse<SpeakingSubmissionResponse>> submitSpeaking(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitSpeakingRequest request,
            HttpServletRequest httpRequest
            ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /exercises/{}/speaking - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        request.setUserId(user.getUserId());

        SpeakingSubmissionResponse response = submissionService.submitSpeaking(id, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<SpeakingSubmissionResponse>builder()
                        .status(201)
                        .success(true)
                        .message("Speaking submission received. Grading in progress.")
                        .data(response)
                        .build());
    }

    // ============================================
    // GET /exercises/{id}/speaking/{submissionId}
    // ============================================

    @Operation(summary = "Poll speaking submission grading status")
    @GetMapping("/{id}/speaking/{submissionId}")
    public ResponseEntity<ApiResponse<SpeakingSubmissionResponse>> getSpeakingSubmission(
            @PathVariable UUID id,
            @PathVariable UUID submissionId,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /exercises/{}/speaking/{} - accountId={}", id, submissionId, accountId);

        SpeakingSubmissionResponse response = submissionService.getSpeakingSubmission(submissionId);

        return ResponseEntity.ok(
                ApiResponse.success("Speaking submission retrieved", response)
        );
    }

    // ============================================
    // POST /exercises/{id}/writing
    // ============================================

    @Operation(summary = "Submit an essay writing",
            description = "Returns immediately with status=PENDING. Poll GET for result.")
    @PostMapping("/{id}/writing")
    public ResponseEntity<ApiResponse<WritingSubmissionResponse>> submitWriting(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitWritingRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("POST /exercises/{}/writing - accountId={}", id, accountId);

        InternalUserResponse user = userServiceClient.getUserByAccountId(accountId);
        request.setUserId(user.getUserId());

        WritingSubmissionResponse response = submissionService.submitWriting(id, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<WritingSubmissionResponse>builder()
                        .status(201)
                        .success(true)
                        .message("Writing submission received. Grading in progress.")
                        .data(response)
                        .build());
    }

    // ============================================
    // GET /exercises/{id}/writing/{submissionId}
    // ============================================

    @Operation(summary = "Poll writing submission grading status")
    @GetMapping("/{id}/writing/{submissionId}")
    public ResponseEntity<ApiResponse<WritingSubmissionResponse>> getWritingSubmission(
            @PathVariable UUID id,
            @PathVariable UUID submissionId,
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /exercises/{}/writing/{} - accountId={}", id, submissionId, accountId);

        WritingSubmissionResponse response = submissionService.getWritingSubmission(submissionId);

        return ResponseEntity.ok(
                ApiResponse.success("Writing submission retrieved", response)
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
