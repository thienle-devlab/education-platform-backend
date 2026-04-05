package com.lethien.user_service.dto;

import com.lethien.user_service.entity.UserSetting;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * UserSettingResponse — returned to the authenticated user only.
 * Never exposed in public profile endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingResponse {

    private String language;
    private String timezone;
    private String theme;
    private String fontSize;

    private Boolean notificationEnabled;
    private Boolean emailNotification;
    private Boolean pushNotification;
    private Boolean smsNotification;

    private ZonedDateTime updatedAt;

    // ============================================
    // MAPPER
    // ============================================

    /**
     * Map from UserSetting entity.
     */
    public static UserSettingResponse from(UserSetting setting) {
        if (setting == null) return null;
        return UserSettingResponse.builder()
                .language(setting.getLanguage())
                .timezone(setting.getTimezone())
                .theme(setting.getTheme())
                .fontSize(setting.getFontSize())
                .notificationEnabled(setting.getNotificationEnabled())
                .emailNotification(setting.getEmailNotification())
                .pushNotification(setting.getPushNotification())
                .smsNotification(setting.getSmsNotification())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
