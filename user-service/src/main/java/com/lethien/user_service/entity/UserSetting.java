package com.lethien.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * UserSetting entity - stores user preferences and settings
 * Maps to: user_settings table in user_db
 */
@Entity
@Table(name = "user_settings", indexes = {
        @Index(name = "idx_user_settings_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_user_settings_user"))
    private User user;

    @Column(name = "language", nullable = false, length = 10)
    @Builder.Default
    private String language = "vi";

    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "notification_enabled", nullable = false)
    @Builder.Default
    private Boolean notificationEnabled = true;

    @Column(name = "email_notification", nullable = false)
    @Builder.Default
    private Boolean emailNotification = true;

    @Column(name = "push_notification", nullable = false)
    @Builder.Default
    private Boolean pushNotification = true;

    @Column(name = "sms_notification", nullable = false)
    @Builder.Default
    private Boolean smsNotification = false;

    @Column(name = "theme", nullable = false, length = 20)
    @Builder.Default
    private String theme = "light";

    @Column(name = "font_size", nullable = false, length = 20)
    @Builder.Default
    private String fontSize = "medium";

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Check if any notifications are enabled
     */
    public boolean hasNotificationsEnabled(){
        return Boolean.TRUE.equals(this.notificationEnabled) &&
                (Boolean.TRUE.equals(this.emailNotification) ||
                        Boolean.TRUE.equals(this.pushNotification) ||
                        Boolean.TRUE.equals(this.smsNotification));
    }

    /**
     * Disable all notifications
     */
    public void disableAllNotifications() {
        this.notificationEnabled = false;
        this.emailNotification = false;
        this.pushNotification = false;
        this.smsNotification = false;
    }

    /**
     * Enable all notifications
     */
    public void enableAllNotifications() {
        this.notificationEnabled = true;
        this.emailNotification = true;
        this.pushNotification = true;
        this.smsNotification = true;
    }

    /**
     * Check if dark theme
     */
    public boolean isDarkTheme() {
        return "dark".equalsIgnoreCase(this.theme);
    }

    /**
     * Toggle theme
     */
    public void toggleTheme() {
        this.theme = isDarkTheme() ? "light" : "dark";
    }
}