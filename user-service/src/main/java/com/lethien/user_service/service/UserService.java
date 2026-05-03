package com.lethien.user_service.service;

import com.lethien.user_service.dto.CreateUserRequest;
import com.lethien.user_service.dto.UpdateProfileRequest;
import com.lethien.user_service.dto.UserPublicResponse;
import com.lethien.user_service.dto.UserResponse;
import com.lethien.user_service.entity.Role;
import com.lethien.user_service.entity.RoleType;
import com.lethien.user_service.entity.User;
import com.lethien.user_service.exception.UserNotFoundException;
import com.lethien.user_service.repository.RoleRepository;
import com.lethien.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * UserService — handles user profile creation and management.
 * Called by AccountEventListener when Auth Service publishes AccountCreatedEvent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;

    // ============================================
    // CREATE
    // ============================================

    /**
     * Create a new user profile from AccountCreatedEvent.
     *
     * Idempotent — if profile already exists (duplicate event), return existing.
     * Assigns STUDENT role by default (mirrors DB trigger assign_default_role).
     *
     * @param request payload from AccountCreatedEvent
     * @return UserProfileResponse of the created (or existing) profile
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user profile: accountId={}, email={}",
                request.getAccountId(), request.getEmail());

        // 1️⃣ Idempotency check (Kafka có thể gửi lại)
        Optional<User> existingUser = userProfileRepository
                .findByAccountId(request.getAccountId());

        if (existingUser.isPresent()) {
            log.warn("User profile already exists for accountId={} — skipping creation",
                    request.getAccountId());

            return UserResponse.from(existingUser.get());
        }

        // 2️⃣ Resolve default role
        Role defaultRole = roleRepository.findByName(RoleType.STUDENT)
                .orElseThrow(() -> {
                    log.error("Default role STUDENT not found — run seed data");
                    return new IllegalStateException("Default role STUDENT not found");
                });

        // 3️⃣ Build NEW entity
        User user = User.builder()
                .accountId(request.getAccountId())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .build();

        // 4️⃣ Save ONCE
        // Persist first so UserRole FK is valid
        User savedUser = userProfileRepository.save(user);

        log.info("User profile created: id={}, email={}, name={}, phone={}", savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getPhoneNumber());

        // Use static mapper from UserProfileResponse — no duplicate mapping logic
        return UserResponse.from(savedUser);
    }

    // ============================================
    // READ
    // ============================================

    /**
     * Get full profile by user ID (= accountId).
     * Used by: GET /users/me, idempotency check in createUser.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return UserResponse.from(user);
    }

    /**
     * Get full profile by accountId from Auth Service.
     * Used by: inter-service lookup, idempotency check.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByAccountId(UUID accountId) {
        User user = userProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("User not found for accountId: " + accountId));

        return UserResponse.from(user);
    }

    /**
     * Get public profile by user ID — visible to other authenticated users.
     * Used by: GET /users/{id}.
     *
     * Excludes sensitive fields: email, phone, dateOfBirth, settings, accountId.
     */
    @Transactional(readOnly = true)
    public UserPublicResponse getPublicProfileById(UUID userId) {
        User user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        return UserPublicResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfileByAccountId(UUID accountId, UpdateProfileRequest request) {
        log.info("Updating profile by accountId={}", accountId);

        User user = userProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new UserNotFoundException("User not found for accountId: " + accountId));

        return applyProfileUpdate(user, request);
    }

    // ============================================
    // UPDATE
    // ============================================

    /**
     * Update profile for the authenticated user.
     *
     * Partial update — only non-null fields in the request are applied.
     * Caller identity (userId) is resolved from JWT at controller level,
     * NOT from the request body.
     *
     * Fields that can be updated:
     *   fullName, phoneNumber, dateOfBirth, avatarUrl, bio
     *
     * Fields intentionally excluded from update:
     *   email    — synced from Auth Service only
     *   accountId — immutable
     *
     * @param userId  ID of the authenticated user (from JWT)
     * @param request partial update payload
     * @return updated UserResponse
     */

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile: userId={}", userId);

        User user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        return applyProfileUpdate(user, request);
    }

    private UserResponse applyProfileUpdate(User user, UpdateProfileRequest request) {
        // Partial update — only apply fields that are provided (non-null)
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        // @PreUpdate on User entity will refresh updatedAt automatically
        User updatedUser = userProfileRepository.save(user);

        log.info("Profile updated: userId={}", updatedUser.getId());

        return UserResponse.from(updatedUser);
    }

    // ============================================
    // EMAIL SYNC — triggered by Kafka event từ Auth Service
    // ============================================

    /**
     * Sync email từ Auth Service sau khi user xác nhận đổi email.
     *
     * Chỉ được gọi bởi EmailChangeEventHandler khi nhận EMAIL_CHANGE_CONFIRMED.
     * Không expose ra controller vì email chỉ được đổi qua Auth Service.
     *
     * Idempotent — nếu email đã đúng rồi thì bỏ qua, không update thừa.
     *
     * @param accountId accountId từ Auth Service
     * @param newEmail  email mới đã được xác nhận
     */
    @Transactional
    public void syncEmail(UUID accountId, String newEmail) {
        log.info("Syncing email: accountId={}, newEmail={}", accountId, newEmail);

        User user = userProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new UserNotFoundException("User not found for accountId: " + accountId));

        // Idempotent check — tránh update thừa nếu email đã đúng
        if (newEmail.equals(user.getEmail())) {
            log.info("Email already synced, skipping. accountId={}", accountId);
            return;
        }

        user.setEmail(newEmail);
        userProfileRepository.save(user);

        log.info("Email synced successfully: accountId={}, newEmail={}", accountId, newEmail);
    }
}
