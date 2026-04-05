package com.lethien.user_service.repository;

import com.lethien.user_service.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, UUID> {

    // ============================================
    // FIND BY USER
    // ============================================

    /**
     * Find settings for a specific user.
     * Used in: loading user preferences, notification checks.
     *
     * Note: in most flows, settings are accessed via userProfile.getSetting().
     * This method is for cases where you have userId but not the full UserProfile.
     */
    Optional<UserSetting> findByUserId(UUID userId);

    /**
     * Check if settings exist for a user.
     * Used in: verifying DB trigger ran correctly after user creation.
     */
    boolean existsByUserId(UUID userId);

    // ============================================
    // BULK QUERIES
    // ============================================

    /**
     * Find all users with a specific language preference.
     * Used in: sending localised bulk notifications.
     */
    @Query("""
        SELECT s FROM UserSetting s
        WHERE s.language = :language
          AND s.notificationEnabled = true
          AND s.emailNotification = true
        """)
    java.util.List<UserSetting> findAllByLanguageWithEmailEnabled(
            @Param("language") String language);

    // ============================================
    // UPDATE SHORTCUTS
    // ============================================

    /**
     * Update language and timezone in one query — avoids loading the full entity
     * just to change two fields.
     *
     * Used in: user preference update endpoint.
     */
    @Modifying
    @Query("""
        UPDATE UserSetting s
        SET s.language = :language,
            s.timezone = :timezone,
            s.updatedAt = CURRENT_TIMESTAMP
        WHERE s.user.id = :userId
        """)
    void updateLocale(
            @Param("userId") UUID userId,
            @Param("language") String language,
            @Param("timezone") String timezone);

    /**
     * Disable all notifications for a user in one query.
     * Used in: account suspension, admin revoke.
     * Mirrors UserSetting.revoke() at the DB level — no entity load needed.
     */
    @Modifying
    @Query("""
        UPDATE UserSetting s
        SET s.notificationEnabled = false,
            s.emailNotification   = false,
            s.pushNotification    = false,
            s.smsNotification     = false,
            s.updatedAt           = CURRENT_TIMESTAMP
        WHERE s.user.id = :userId
        """)
    void revokeAllNotifications(@Param("userId") UUID userId);
}
