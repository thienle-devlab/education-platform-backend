package com.lethien.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response DTO after successful registration
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResponse {
    /**
     * Account ID
     */
    private UUID accountId;

    /**
     * Email address
     */
    private String email;

    /**
     * Account status
     */
    private String status;

    /**
     * Email verified status
     */
    private Boolean emailVerified;

    /**
     * Full name
     */
    private String fullName;

    /**
     * Registration timestamp
     */
    private ZonedDateTime createdAt;

    /**
     * Message to user (e.g., "Please check your email to verify your account")
     */
    private String message;
}
