package com.lethien.user_service.dto;

import com.lethien.user_service.entity.User;
import lombok.*;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * InternalUserResponse — lightweight DTO cho inter-service calls.
 *
 * Chỉ expose các field cần thiết cho Course Service:
 *   - userId, accountId    → identity mapping
 *   - roles                → phân quyền (STUDENT / INSTRUCTOR / ADMIN)
 *   - isInstructor         → shortcut check
 *   - fullName, email      → hiển thị trong response của Course Service
 *   - avatarUrl            → hiển thị avatar instructor trong catalog
 *
 * KHÔNG expose: dateOfBirth, phoneNumber, bio, settings
 * Used by: InternalUserController → Course Service (UserServiceClient)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalUserResponse {

    private UUID userId;
    private UUID accountId;

    private String fullName;
    private String email;
    private String avatarUrl;

    /** Tập hợp tên role: "STUDENT", "INSTRUCTOR", "ADMIN" */
    private Set<String> roles;

    /** Shortcut — true nếu có role INSTRUCTOR hoặc ADMIN */
    private boolean isInstructor;

    /** Shortcut — true nếu có role ADMIN */
    private boolean isAdmin;

    // ============================================
    // MAPPER
    // ============================================

    public static InternalUserResponse from(User user) {
        Set<String> roleNames = user.getRoleTypes().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return InternalUserResponse.builder()
                .userId(user.getId())
                .accountId(user.getAccountId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleNames)
                .isInstructor(user.isInstructor())
                .isAdmin(user.isAdmin())
                .build();
    }
}
