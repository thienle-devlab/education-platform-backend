package com.lethien.auth_service.entity;

import lombok.Getter;

/**
 * OAuth provider enum
 * Maps to: oauth_provider enum in database
 */
@Getter
public enum OAuthProvider {
    GOOGLE("Google"),
    FACEBOOK("Facebook"),
    GITHUB("GitHub");


    private final String displayName;

    OAuthProvider(String displayName) {
        this.displayName = displayName;
    }
}
