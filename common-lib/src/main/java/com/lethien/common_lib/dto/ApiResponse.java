package com.lethien.common_lib.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * Standard API response wrapper
 * Used by all microservices for consistent response format
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {
    /**
     * HTTP status code
     */
    @Schema(
            description = "HTTP status code",
            example = "200",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer status;

    /**
     * Success indicator
     */
    @Schema(
            description = "Success indicator",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean success;

    /**
     * Response message
     */
    @Schema(
            description = "Response message",
            example = "Operation completed successfully"
    )
    private String message;

    /**
     * Response data (generic type)
     */
    @Schema(
            description = "Response data payload",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private T data;

    /**
     * Error details (only present on failure)
     */
    @Schema(
            description = "Error details (only present when success=false)",
            nullable = true
    )
    private ErrorDetails error;

    /**
     * Timestamp
     */
    @Schema(
            description = "Response timestamp in ISO-8601 format",
            example = "2026-03-08T23:30:00+07:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Builder.Default
    private ZonedDateTime timestamp = ZonedDateTime.now();

    // ============================================
    // STATIC FACTORY METHODS
    // ============================================

    /**
     * Create success response with data
     */
    public static <T> ApiResponse<T> success(T data){
        return ApiResponse.<T>builder()
                .status(200)
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Create success response with custom message
     */
    public static <T> ApiResponse<T> success(String message, T data){
        return ApiResponse.<T>builder()
                .status(200)
                .success(true)
                .message(message)
                .data(data)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Create success response without data
     */
    public static <T> ApiResponse<T> success(String message){
        return ApiResponse.<T>builder()
                .status(200)
                .success(true)
                .message(message)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Create error response
     */
    public static <T> ApiResponse<T> error(Integer status, String message, ErrorDetails error){
        return ApiResponse.<T>builder()
                .status(status)
                .success(false)
                .message(message)
                .error(error)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Create error response with simple message
     */
    public static <T> ApiResponse<T> error(Integer status, String message) {
        return error(status, message, ErrorDetails.of("ERROR", message));
    }

    /**
     * Create error response with details
     */
    public static <T> ApiResponse<T> error(int status, String message, ErrorDetails error){
        return ApiResponse.<T>builder()
                .status(status)
                .success(false)
                .message(message)
                .error(error)
                .build();
    }

    /**
     * Create validation error response
     */
    public static <T> ApiResponse<T> validationError(ErrorDetails error){
        return ApiResponse.<T>builder()
                .status(400)
                .success(false)
                .message("Validation failed")
                .error(error)
                .build();
    }
}
