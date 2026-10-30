package com.lethien.course_service.exception;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.common_lib.dto.ErrorDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — catches all exceptions thrown by Course Service controllers.
 *
 * HTTP status mapping:
 *   400 — Validation errors, IllegalArgument, IllegalState
 *   403 — CourseAccessDeniedException, NotEnrolledException
 *   404 — CourseNotFoundException, EnrollmentNotFoundException,
 *          ExerciseNotFoundException, LessonNotFoundException, SubmissionNotFoundException
 *   409 — SlugAlreadyExistsException, AlreadyEnrolledException, CourseAlreadyPublishedException
 *   422 — CoursePublishException (precondition not met)
 *   500 — Unexpected errors
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ============================================
    // 400 — BAD REQUEST
    // ============================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        log.error("Validation error: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed")
                .fieldErrors(fieldErrors)
                .path(extractPath(request))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "Validation failed", errorDetails));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        log.error("Illegal argument: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("INVALID_ARGUMENT", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex,
            WebRequest request
    ) {
        log.error("Illegal state: {}", ex.getMessage());

        ErrorDetails errorDetails =  ErrorDetails.of("INVALID_STATE", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage(), errorDetails));
    }

    // ============================================
    // 403 — FORBIDDEN
    // ============================================

    @ExceptionHandler(CourseAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourseAccessDenied(
            CourseAccessDeniedException ex,
            WebRequest request
    ) {
        log.error("Course access denied: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("COURSE_ACCESS_DENIED", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(NotEnrolledException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotEnrolled(
            NotEnrolledException ex,
            WebRequest request
    ) {
        log.error("Not enrolled: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("NOT_ENROLLED", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(NotEligibleToReviewException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotEligibleToReview(
            NotEligibleToReviewException ex,
            WebRequest request
    ) {
        log.error("Not eligible to review: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("NOT_ELIGIBLE_TO_REVIEW", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, ex.getMessage(), errorDetails));
    }

    // ============================================
    // 404 — NOT FOUND
    // ============================================

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourseNotFound(
            CourseNotFoundException ex,
            WebRequest request
    ) {
        log.error("Course not found: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("COURSE_NOT_FOUND", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(EnrollmentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEnrollmentNotFound(
            EnrollmentNotFoundException ex,
            WebRequest request
    ) {
        log.error("Enrollment not found: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("ENROLLMENT_NOT_FOUND", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleExerciseNotFound(
            ExerciseNotFoundException ex,
            WebRequest request
    ) {
        log.error("Exercise not found: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("EXERCISE_NOT_FOUND", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(LessonNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleLessonNotFound(
            LessonNotFoundException ex,
            WebRequest request
    ) {
        log.error("Lesson not found: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("LESSON_NOT_FOUND", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSubmissionNotFound(
            SubmissionNotFoundException ex,
            WebRequest request
    ) {
        log.error("Submission not found: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("SUBMISSION_NOT_FOUND", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleReviewNotFound(
            ReviewNotFoundException ex,
            WebRequest request
    ) {
        log.error("Review not found: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("REVIEW_NOT_FOUND", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(SectionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSectionNotFound(
            SectionNotFoundException ex, WebRequest request
    ) {
        log.error("Section not found: {}", ex.getMessage());
        ErrorDetails errorDetails = ErrorDetails.of("SECTION_NOT_FOUND", ex.getMessage());
        errorDetails.setPath(extractPath(request));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage(), errorDetails));
    }

    // ============================================
    // 409 — CONFLICT
    // ============================================

    @ExceptionHandler(SlugAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleSlugAlreadyExists(
            SlugAlreadyExistsException ex,
            WebRequest request
    ) {
        log.error("Slug already exists: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("SLUG_EXISTS", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(AlreadyEnrolledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyEnrolled(
            AlreadyEnrolledException ex,
            WebRequest request
    ) {
        log.error("Already enrolled: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("ALREADY_ENROLLED", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(CourseAlreadyPublishedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCourseAlreadyPublished(
            CourseAlreadyPublishedException ex,
            WebRequest request
    ) {
        log.error("Course already published: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("COURSE_ALREADY_PUBLISHED", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(AlreadyReviewedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyReviewed(
            AlreadyReviewedException ex,
            WebRequest request
    ) {
        log.error("Already reviewed: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("ALREADY_REVIEWED", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, ex.getMessage(), errorDetails));
    }

    // ============================================
    // 422 — UNPROCESSABLE ENTITY
    // ============================================

    /**
     * Precondition not met — e.g. publish course with no lessons, delete published course.
     */
    @ExceptionHandler(CoursePublishException.class)
    public ResponseEntity<ApiResponse<Void>> handleCoursePublishException(
            CoursePublishException ex,
            WebRequest request
    ) {
        log.error("Course publish/delete precondition failed: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("COURSE_PUBLISH_ERROR", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(422, ex.getMessage(), errorDetails));
    }

    @ExceptionHandler(ContentModificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleContentModification(
            ContentModificationException ex, WebRequest request) {
        log.error("Content modification blocked: {}", ex.getMessage());
        ErrorDetails errorDetails = ErrorDetails.of("CONTENT_MODIFICATION_BLOCKED", ex.getMessage());
        errorDetails.setPath(extractPath(request));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(422, ex.getMessage(), errorDetails));
    }

    // ============================================
    // 503 — SERVICE UNAVAILABLE
    // ============================================

    /**
     * Handle UserServiceException — không thể gọi User Service.
     * Trả 503 để client biết đây là lỗi tạm thời, có thể retry.
     */
    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserServiceException(
            UserServiceException ex,
            WebRequest request
    ) {
        log.error("User Service unavailable: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of(
                "USER_SERVICE_UNAVAILABLE", ex.getMessage());
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, ex.getMessage(), errorDetails));
    }

    // ============================================
    // 500 — INTERNAL SERVER ERROR
    // ============================================

    /**
     * Catch-all — handles any unexpected exception not covered above.
     * Logs full stack trace, returns generic message to client (no internal detail exposed).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex,
            WebRequest request
    ) {
        log.error("Unexpected error at {}: ", extractPath(request), ex);

        ErrorDetails errorDetails = ErrorDetails.of(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later."
        );
        errorDetails.setPath(extractPath(request));

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Internal server error", errorDetails));
    }

    // ============================================
    // PRIVATE HELPER
    // ============================================

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
