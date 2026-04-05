package com.lethien.user_service.dto;

import com.lethien.user_service.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * UserProfileResponse — full profile returned to the authenticated user
 * viewing their own account (GET /users/me).
 *
 * Includes sensitive fields (email, phone, dateOfBirth, settings, roles)
 * that are NOT exposed in the public profile.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private  UUID accountId;

    // Identity
    private String email;
    private String fullName;
    private String firstName;
    private String lastName;

    // Profile
    private String avatarUrl;
    private String bio;
    private LocalDate dateOfBirth;
    private Integer age;
    private String phoneNumber;

    // Roles
    private List<RoleResponse> roles;

    // Settings (private — only visible to self)
    private UserSettingResponse setting;

    // Audit
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    // ============================================
    // MAPPER
    // ============================================

    /**
     * Map from User entity.
     * Calls getFirstName() / getLastName() / getAge() business methods
     * defined on the entity — no duplication of logic here.
     */
    public static UserResponse from(User user) {
        List<RoleResponse> roles = user.getUserRoles().stream()
                .map(RoleResponse::from)
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .accountId(user.getAccountId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .dateOfBirth(user.getDateOfBirth())
                .age(user.getAge())
                .phoneNumber(user.getPhoneNumber())
                .roles(roles)
                .setting(UserSettingResponse.from(user.getUserSetting()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
