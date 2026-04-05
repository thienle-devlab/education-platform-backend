package com.lethien.user_service.dto;

import com.lethien.user_service.entity.RoleType;
import com.lethien.user_service.entity.UserRole;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * RoleResponse — role assignment info returned inside UserProfileResponse.
 *
 * Includes assignment metadata (assignedAt, autoAssigned)
 * so the client knows when and how a role was granted.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private RoleType roleType;

    private String displayName;

    private ZonedDateTime assignedAt;

    /**
     * True if assigned by the system (assignedBy == null).
     * False if assigned manually by an admin.
     */
    private boolean autoAssigned;

    // ============================================
    // MAPPER
    // ============================================

    /**
     * Map from UserRole entity.
     */
    public static RoleResponse from(UserRole userRole) {
        return RoleResponse.builder()
                .roleType(userRole.getRole().getName())
                .displayName(userRole.getRole().getName().getDisplayName())
                .assignedAt(userRole.getAssignedAt())
                .autoAssigned(userRole.isAutoAssigned())
                .build();
    }
}
