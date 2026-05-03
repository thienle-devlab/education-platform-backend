package com.lethien.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailChangeRequest {

    @NotBlank(message = "New email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String newEmail;

    @NotBlank(message = "Current password is required")
    private String currentPassword;  // Bắt buộc xác nhận mật khẩu trước khi đổi email
}
