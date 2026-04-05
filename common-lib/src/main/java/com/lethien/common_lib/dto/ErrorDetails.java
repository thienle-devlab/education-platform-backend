package com.lethien.common_lib.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Error details for API responses
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detailed error information")
public class ErrorDetails {
    /**
     * Error code (e.g., "AUTH001", "VALIDATION_ERROR")
     */
    @Schema(
            description = "Error code (machine-readable)",
            example = "VALIDATION_ERROR",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    /**
     * Error message
     */
    @Schema(
            description = "Error message (human-readable)",
            example = "Validation failed for one or more fields",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;

    /**
     * Detailed error description
     */
    @Schema(
            description = "Additional error details",
            example = "Field 'email' is required"
    )
    private String details;

    /**
     * Field-level validation errors (field name -> error message)
     */
    @Schema(
            description = "Field-level validation errors",
            example = "{\"email\": \"Email is required\", \"password\": \"Password must be at least 8 characters\"}"
    )
    private Map<String, String> fieldErrors;

    /**
     * List of validation errors
     */
    @Schema(
            description = "List of error messages",
            example = "[\"Email already exists\", \"Password is too weak\"]"
    )
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * Request path that caused the error
     */
    @Schema(
            description = "API endpoint path where error occurred",
            example = "/api/auth/register"
    )
    private String path;

    // ============================================
    // STATIC FACTORY METHODS
    // ============================================

    /**
     * Create simple error details
     */
    public static ErrorDetails of(String code, String message){
        return ErrorDetails.builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * Create error details with field errors
     */
    public static ErrorDetails of(String code, String message, Map<String, String> fieldErrors){
        return ErrorDetails.builder()
                .code(code)
                .message(message)
                .fieldErrors(fieldErrors)
                .build();
    }

    /**
     * Add field error
     */
    public void addFieldError(String field, String error){
        if (this.fieldErrors == null) {
            this.fieldErrors = new java.util.HashMap<>();
        }
        this.fieldErrors.put(field, error);
    }

    /**
     * Add general error
     */
    public void addError(String error){
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
}
