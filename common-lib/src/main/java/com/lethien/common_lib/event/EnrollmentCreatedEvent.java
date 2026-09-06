package com.lethien.common_lib.event;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * EnrollmentCreatedEvent
 *
 * Published by: Course Service — when a user successfully enrolls in a course.
 * Consumed by:  Notification Service — send welcome email.
 *               Analytics Service    — track enrollment metrics.
 *
 * Transport: Kafka topic "course.enrollment.created"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentCreatedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ============================================
    // EVENT METADATA
    // ============================================

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Builder.Default
    private String eventType = "ENROLLMENT_CREATED";

    @Builder.Default
    private ZonedDateTime occurredAt = ZonedDateTime.now();

    // ============================================
    // PAYLOAD
    // ============================================

    private UUID enrollmentId;

    /** Soft ref → users.id in User Service */
    private UUID userId;

    private UUID courseId;
    private String courseTitle;
    private String courseSkillType;  // "LISTENING" | "SPEAKING" | "READING" | "WRITING"

    private ZonedDateTime enrolledAt;

    // ============================================
    // BUSINESS METHODS
    // ============================================

    public String idempotencyKey() {
        return this.eventType + ":" + this.enrollmentId;
    }

    public boolean isValid() {
        return this.enrollmentId != null
                && this.userId != null
                && this.courseId != null;
    }
}
