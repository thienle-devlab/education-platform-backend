package com.lethien.user_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * UpdateSettingRequest — sent by the authenticated user to update their preferences.
 *
 * All fields are optional (null = keep existing value).
 * Validation mirrors DB CHECK constraints in 01-init.sql.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSettingRequest {

    @Pattern(
            regexp = "^(vi|en|ja|ko|zh)$",
            message = "Language must be one of: vi, en, ja, ko, zh"
    )
    private String language;

    /**
     * Timezone string.
     * Deep validation (is it a real TZ?) done in service layer via ZoneId.of().
     */
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    @Pattern(
            regexp = "^(light|dark|auto)$",
            message = "Theme must be one of: light, dark, auto"
    )
    private String theme;

    @Pattern(
            regexp = "^(small|medium|large)$",
            message = "Font size must be one of: small, medium, large"
    )
    private String fontSize;

    private Boolean notificationEnabled;

    private Boolean emailNotification;

    private Boolean pushNotification;

    private Boolean smsNotification;
}
