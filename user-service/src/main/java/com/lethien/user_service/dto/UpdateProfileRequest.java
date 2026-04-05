package com.lethien.user_service.dto;


import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * UpdateProfileRequest — sent by the authenticated user to update their own profile.
 *
 * All fields are optional (null = keep existing value).
 * Caller identity (userId) comes from the JWT token at the controller level,
 * NOT from this payload — prevents users from updating other people's profiles.
 *
 * Validation mirrors DB CHECK constraints in 01-init.sql.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Phone number must be 10–15 digits, optionally starting with +"
    )
    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String avatarUrl;

    private String bio;
}
