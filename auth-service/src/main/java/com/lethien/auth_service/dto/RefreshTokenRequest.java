package com.lethien.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for refreshing access token
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
