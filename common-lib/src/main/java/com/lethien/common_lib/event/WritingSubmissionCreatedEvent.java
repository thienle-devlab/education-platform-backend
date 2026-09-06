package com.lethien.common_lib.event;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * WritingSubmissionCreatedEvent
 *
 * Published by: Course Service — when a user submits an essay.
 * Consumed by:  AI Service     — grammar, vocabulary, coherence, task achievement scoring.
 *
 * Transport: Kafka topic "course.writing.submission.created"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WritingSubmissionCreatedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ============================================
    // EVENT METADATA
    // ============================================

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Builder.Default
    private String eventType = "WRITING_SUBMISSION_CREATED";

    @Builder.Default
    private ZonedDateTime occurredAt = ZonedDateTime.now();

    // ============================================
    // PAYLOAD
    // ============================================

    private UUID submissionId;
    private UUID userId;
    private UUID exerciseId;
    private Integer attemptNumber;

    /** Nội dung bài viết — AI Service sẽ phân tích trực tiếp */
    private String content;

    private Integer wordCount;

    private ZonedDateTime submittedAt;

    // ============================================
    // BUSINESS METHODS
    // ============================================

    public String idempotencyKey() {
        return this.eventType + ":" + this.submissionId;
    }

    public boolean isValid() {
        return this.submissionId != null
                && this.userId != null
                && this.exerciseId != null
                && this.content != null && !this.content.isBlank();
    }
}
