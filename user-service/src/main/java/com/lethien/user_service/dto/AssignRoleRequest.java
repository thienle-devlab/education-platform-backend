package com.lethien.user_service.dto;

import com.lethien.user_service.entity.RoleType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * AssignRoleRequest — sent by an ADMIN to assign or revoke a role from a user.
 *
 * targetUserId comes from the path variable at the controller level:
 *   PATCH /admin/users/{userId}/roles
 *
 * assignedBy is resolved from the JWT token at the controller level,
 * NOT from this payload — prevents privilege escalation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRoleRequest {

    @NotNull(message = "roleType is required")
    private RoleType roleType;

    private String reason;
}
