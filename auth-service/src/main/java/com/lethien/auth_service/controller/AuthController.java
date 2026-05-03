package com.lethien.auth_service.controller;

import com.lethien.auth_service.dto.*;
import com.lethien.auth_service.service.AuthService;
import com.lethien.common_lib.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for authentication endpoints
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and Authorization APIs")
public class AuthController {

    private final AuthService authService;

    /**
     * Register new account
     *
     * @param request Registration request containing email, password, full name
     * @return ApiResponse with registration details
     */
    @Operation(
            summary = "Register new account",
            description = "Create a new user account. Email verification required before login."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Registration successful",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email already exists"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error"
            )
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")  // No auth required
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("POST /api/auth/register - email: {}", request.getEmail());

        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    /**
     * Login with email and password
     *
     * @param request Login request with email and password
     * @param httpRequest HTTP request for IP and User-Agent
     * @return ApiResponse with JWT tokens
     */
    @Operation(
            summary = "Login",
            description = "Authenticate user with email and password. Returns access token and refresh token."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Account locked or not verified"
            )
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")  // No auth required
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
            ) {
        log.info("POST /api/auth/login - email: {}", request.getEmail());

        // Get IP address and User-Agent
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = authService.login(request, ipAddress, userAgent);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response)
        );
    }

    // TODO: Xóa tài khoản rác nếu sau thời gian hết hạn người dùng không kích hoạt email đăng kí tài khoản

    /**
     * Verify email with token
     *
     * @param token Verification token from email
     * @return ApiResponse with success message
     */
    @Operation(
            summary = "Verify email",
            description = "Verify user email with verification token sent via email"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Email verified successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired token"
            )
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")  // No auth required
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam("token") String token
    ) {
        log.info("GET /api/auth/verify-email - token: {}", token);

        authService.verifyEmail(token);

        return ResponseEntity.ok(
                ApiResponse.success("Email verified successfully")
        );
    }

    /**
     * Refresh access token using refresh token
     *
     * @param request Refresh token request
     * @return ApiResponse with new access token
     */
    @Operation(
            summary = "Refresh access token",
            description = "Get new access token using valid refresh token"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token"
            )
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")  // No auth required (uses refresh token)
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
            ) {
        log.info("POST /api/auth/refresh-token");

        LoginResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", response)
        );
    }

    /**
     * Logout (revoke refresh token)
     *
     * @param request Refresh token to revoke
     * @return ApiResponse with success message
     */
    /**
     * Logout (revoke refresh token)
     */
    @Operation(
            summary = "Logout",
            description = "Revoke refresh token and logout user from current session"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Logout successful"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid refresh token"
            )
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")  // No auth required (uses refresh token)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("POST /api/auth/logout");

        authService.logout(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("Logout successful")
        );
    }

    /**
     * Resend verification email
     *
     * @param email Email address to resend verification to
     * @return ApiResponse with success message
     */
    @Operation(
            summary = "Resend verification email",
            description = "Resend email verification link to user's email"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Verification email sent"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Email already verified or account not found"
            )
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")  // No auth required
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam("email") String email
    ) {
        log.info("POST /api/auth/resend-verification - email: {}", email);

        authService.resendVerificationEmail(email);

        return ResponseEntity.ok(
                ApiResponse.success("Verification email sent")
        );
    }

    /**
     * Request email change
     */
    @Operation(
            summary = "Request email change",
            description = "Request to change account email. Verification link will be sent to the new email address."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Verification email sent to new address",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or same email"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid current password"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email already exists"
            )
    })
    @PostMapping("/email/change")
    public ResponseEntity<ApiResponse<EmailChangeResponse>> requestEmailChange(
            @Valid @RequestBody EmailChangeRequest request
    ) {
        UUID accountId = getCurrentAccountId();
        log.info("POST /api/auth/email/change - accountId: {}", accountId);

        EmailChangeResponse response = authService.requestEmailChange(accountId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Verification email sent to " + request.getNewEmail(), response)
        );
    }

    /**
     * Confirm email change with token
     */
    @Operation(
            summary = "Confirm email change",
            description = "Confirm email change using verification token sent to the new email address."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Email changed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired token"
            )
    })
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")  // No auth required — dùng token trong link
    @GetMapping("/email/change/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmEmailChange(
            @RequestParam("token") String token
    ) {
        log.info("GET /api/auth/email/change/confirm - token: {}", token);

        authService.confirmEmailChange(token);

        return ResponseEntity.ok(
                ApiResponse.success("Email changed successfully")
        );
    }

    /**
     * Cancel pending email change
     */
    @Operation(
            summary = "Cancel email change",
            description = "Cancel a pending email change request."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Email change cancelled"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "No pending email change request found"
            )
    })
    @DeleteMapping("/email/change")
    public ResponseEntity<ApiResponse<Void>> cancelEmailChange(
    ) {
        UUID accountId = getCurrentAccountId();
        log.info("DELETE /api/auth/email/change - accountId: {}", accountId);

        authService.cancelEmailChange(accountId);

        return ResponseEntity.ok(
                ApiResponse.success("Email change cancelled")
        );
    }

    /**
     * Resend email change verification
     */
    @Operation(
            summary = "Resend email change verification",
            description = "Resend verification link to the pending new email address."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Verification email resent",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "No pending email change request found"
            )
    })
    @PostMapping("/email/change/resend")
    public ResponseEntity<ApiResponse<EmailChangeResponse>> resendEmailChangeVerification(
            @RequestAttribute("accountId") UUID accountId
    ) {
        log.info("POST /api/auth/email/change/resend - accountId: {}", accountId);

        EmailChangeResponse response = authService.resendEmailChangeVerification(accountId);

        return ResponseEntity.ok(
                ApiResponse.success("Verification email resent", response)
        );
    }

    /**
     * Health check endpoint
     */
    @Operation(
            summary = "Health check",
            description = "Check if auth service is running"
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")  // No auth required
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> health() {
        return ResponseEntity.ok(
                ApiResponse.success("Auth service is running"));
    }

    /**
     * Get client IP address from request
     * Handles X-Forwarded-For header for proxied requests
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Get current authenticated account ID from JWT
     * Principal is set by JwtAuthenticationFilter as UUID
     */
    private UUID getCurrentAccountId () {
        return (UUID) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
