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

    // ============================================
    // COURSE SERVICE — Enrollment events
    // ============================================

    /**
     * Published when a user successfully enrolls in a course.
     * Consumed by: Notification Service (send welcome email), Analytics Service.
     */
    public static final String COURSE_ENROLLMENT_CREATED = "course.enrollment.created";

    /**
     * Published when a user completes a course (progress = 100%).
     * Consumed by: Notification Service (send certificate), Analytics Service.
     */
    public static final String COURSE_ENROLLMENT_COMPLETED = "course.enrollment.completed";

    // ============================================
    // COURSE SERVICE — Submission events (AI grading)
    // ============================================

    /**
     * Published when a user submits a SPEAKING exercise.
     * Consumed by: AI Service — speech recognition + pronunciation scoring.
     */
    public static final String COURSE_SPEAKING_SUBMISSION_CREATED = "course.speaking.submission.created";

    /**
     * Published when a user submits a WRITING (ESSAY) exercise.
     * Consumed by: AI Service — grammar, vocabulary, coherence scoring.
     */
    public static final String COURSE_WRITING_SUBMISSION_CREATED = "course.writing.submission.created";

    /**
     * Published by AI Service after grading is complete.
     * Consumed by: Course Service — update submission status + sync score to ExerciseAttempt.
     */
    public static final String COURSE_SUBMISSION_GRADED = "course.submission.graded";
}
