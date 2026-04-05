package com.lethien.auth_service.entity;

/**
 * Account status enum
 * Maps to: account_status enum in database
 */
public enum AccountStatus {
    /**
     * Account created but email not verified yet
     */
    PENDING,

    /**
     * Account active and can login
     */
    ACTIVE,

    /**
     * Account locked due to security reasons (e.g., too many failed login attempts)
     */
    LOCKED,

    /**
     * Account suspended by admin
     */
    SUSPENDED
}
