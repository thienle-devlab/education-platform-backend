package com.lethien.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response DTO after successful login
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

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
     * JWT access token (short-lived)
     */
    private String accessToken;

    /**
     * JWT refresh token (long-lived)
     */
    private String refreshToken;

    /**
     * Token type (always "Bearer")
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access token expiration in milliseconds
     */
    private Long expiresIn;

    /**
     * Login timestamp
     */
    private ZonedDateTime loginAt;
}
