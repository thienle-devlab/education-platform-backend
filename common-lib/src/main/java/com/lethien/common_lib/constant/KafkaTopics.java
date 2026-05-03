package com.lethien.common_lib.constant;

/**
 * KafkaTopics — centralised topic name constants for all services.
 *
 * Placed in lib-common so both producers (Auth Service) and
 * consumers (User Service, etc.) reference the same constants
 * without duplication or typo risk.
 *
 * Naming convention: {service}.{entity}.{event}
 * - Dots separate domain segments (standard Kafka convention)
 * - All lowercase
 */

public class KafkaTopics {

    private KafkaTopics() {}

    // ============================================
    // AUTH SERVICE — Account events
    // ============================================

    /** Published when a new account is registered */
    public static final String ACCOUNT_CREATED = "auth.account.created";

    /** Published when account details change (email, status) */
    public static final String ACCOUNT_UPDATED = "auth.account.updated";

    /** Published when an account is deleted */
    public static final String ACCOUNT_DELETED = "auth.account.deleted";

    // ============================================
    // AUTH SERVICE — Email change events
    // ============================================
    public static final String EMAIL_CHANGE_REQUESTED  = "auth.email.change.requested";
    public static final String EMAIL_CHANGE_CONFIRMED  = "auth.email.change.confirmed";
    public static final String EMAIL_CHANGE_CANCELLED  = "auth.email.change.cancelled";
}
