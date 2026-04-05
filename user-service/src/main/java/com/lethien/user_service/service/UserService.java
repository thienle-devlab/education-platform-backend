package com.lethien.user_service.service;

import com.lethien.user_service.dto.CreateUserRequest;
import com.lethien.user_service.dto.UserResponse;
import com.lethien.user_service.entity.Role;
import com.lethien.user_service.entity.RoleType;
import com.lethien.user_service.entity.User;
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
}
