package com.lethien.user_service.dto;

import com.lethien.user_service.entity.RoleType;
import com.lethien.user_service.entity.User;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * UserPublicResponse — minimal profile visible to other authenticated users.
 * (GET /users/{id})
 *
 * Intentionally excludes: email, phone, dateOfBirth, accountId,
 * settings, assignedBy, assignedAt — information that should not
 * be exposed publicly.
 *
 * Includes only role types (not metadata) so other users can see
 * if someone is an instructor without seeing when/how the role was assigned.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPublicResponse {

    private UUID id;

    private String fullName;
    private String firstName;
    private String lastName;

    private String avatarUrl;
    private String bio;

    /**
     * Only role type names — no assignment metadata exposed publicly.
     */
    private List<RoleType> roles;

    // ============================================
    // MAPPER
    // ============================================

    /**
     * Map from User entity.
     */
    public static UserPublicResponse from(User user) {
        List<RoleType> roleTypes = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return UserPublicResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .roles(roleTypes)
                .build();
    }
}
