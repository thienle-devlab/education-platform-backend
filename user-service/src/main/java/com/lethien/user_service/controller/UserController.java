package com.lethien.user_service.controller;

import com.lethien.common_lib.dto.ApiResponse;
import com.lethien.common_lib.security.JwtService;
import com.lethien.user_service.dto.UpdateProfileRequest;
import com.lethien.user_service.dto.UserPublicResponse;
import com.lethien.user_service.dto.UserResponse;
import com.lethien.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for user profile endpoints.
 *
 * userId is always resolved from the JWT token — never from the request body
 * — to prevent users from modifying other people's profiles.
 *
 * Base path: /users  (configured via application.yml or @RequestMapping below)
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "User profile management APIs")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    // ============================================
    // GET /users/me — authenticated user's own full profile
    // ============================================

    /**
     * Get the authenticated user's own full profile.
     * Includes sensitive fields: email, phone, dateOfBirth, settings, roles.
     *
     * @param httpRequest HTTP request to extract Bearer token
     * @return ApiResponse with full UserResponse
     */
    @Operation(
            summary = "Get my profile",
            description = "Get the authenticated user's full profile including settings and roles."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing or invalid JWT"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User profile not found"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            HttpServletRequest httpRequest
    ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("GET /users/me - accountId={}", accountId);

        UserResponse response = userService.getUserByAccountId(accountId);

        return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved successfully", response)
        );
    }

    // ============================================
    // PATCH /users/me — update authenticated user's own profile
    // ============================================

    /**
     * Update the authenticated user's own profile.
     *
     * Partial update — send only the fields you want to change.
     * Null fields are ignored (existing values are preserved).
     *
     * Fields that CAN be updated: fullName, phoneNumber, dateOfBirth, avatarUrl, bio.
     * Fields that CANNOT be updated via this endpoint: email, accountId.
     *
     * @param httpRequest HTTP request to extract Bearer token
     * @param request     partial update payload
     * @return ApiResponse with updated UserResponse
     */
    @Operation(
            summary = "Update my profile",
            description = "Partially update the authenticated user's profile. Only non-null fields are applied."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation error — check field constraints"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing or invalid JWT"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User profile not found"
            )
    })
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            HttpServletRequest httpRequest,
            @Valid @RequestBody UpdateProfileRequest request
            ) {
        UUID accountId = extractAccountId(httpRequest);
        log.info("PATCH /users/me - accountId={}", accountId);

        UserResponse response = userService.updateProfileByAccountId(accountId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully", response)
        );
    }

    // ============================================
    // GET /users/{id} — public profile of any user
    // ============================================

    /**
     * Get the public profile of any user by their ID.
     * Excludes sensitive fields: email, phone, dateOfBirth, settings, accountId.
     *
     * @param userId target user's ID (user_service internal id)
     * @return ApiResponse with UserPublicResponse
     */
    @Operation(
            summary = "Get user public profile",
            description = "Get another user's public profile. Sensitive fields are excluded."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Public profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserPublicResponse>> getPublicProfile(
            @PathVariable("id") UUID userId
    ) {
        log.info("GET /users/{} - public profile", userId);

        UserPublicResponse response = userService.getPublicProfileById(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Public profile retrieved successfully", response)
        );
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    /**
     * Extract accountId from Bearer token in Authorization header.
     *
     * Header format: "Authorization: Bearer <token>"
     * JwtService.getAccountIdFromToken() parses the "accountId" claim
     * set by Auth Service when the token was issued.
     *
     * @param request incoming HTTP request
     * @return UUID accountId of the authenticated user
     */
    private UUID extractAccountId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7); // strip "Bearer "
        return jwtService.getAccountIdFromToken(token);
    }
}
