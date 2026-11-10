package com.lethien.course_service.client;

import lombok.*;

import java.util.Set;
import java.util.UUID;

/**
 * InternalUserResponse — mirror DTO nhận từ User Service.
 *
 * Mapping với com.lethien.user_service.dto.InternalUserResponse.
 * Giữ đồng bộ khi User Service thêm/sửa field.
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

    private Set<String> roles;

    private boolean isInstructor;
    private boolean isAdmin;
}
