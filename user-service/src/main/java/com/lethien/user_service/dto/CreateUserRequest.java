package com.lethien.user_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

/**
 * CreateUserRequest — internal request to create a new user profile.
 *
 * This is NOT called directly by the end user.
 * Flow: Auth Service registers account → publishes event → User Service
 *       receives this payload and creates the User profile.
 *
 * Required fields mirror the NOT NULL constraints in the users table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotNull(message = "AccountId is required")
    private UUID accountId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 255,  message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Phone number must be valid (10-15 digits, optional + prefix)"
    )
    private String phoneNumber;
}
