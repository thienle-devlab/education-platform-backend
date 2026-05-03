package com.lethien.user_service.exception;

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
 * Global exception handler for User Service controllers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ============================================
    // VALIDATION
    // ============================================

    /**
     * Handle @Valid field validation errors → 400
     */
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
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "Validation failed", errorDetails));
    }

    // ============================================
    // USER-SERVICE SPECIFIC EXCEPTIONS
    // ============================================

    /**
     * Handle UserNotFoundException → 404
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(
            UserNotFoundException ex,
            WebRequest request
    ) {
        log.error("User not found: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.of("USER_NOT_FOUND", ex.getMessage());
        errorDetails.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage(), errorDetails));
    }

    /**
     * Catch-all handler → 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error: ", ex);

        ErrorDetails errorDetails = ErrorDetails.of(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later."
        );
        errorDetails.setPath(request.getDescription(false).replace("uri=", ""));

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Internal server error", errorDetails));
    }
}
