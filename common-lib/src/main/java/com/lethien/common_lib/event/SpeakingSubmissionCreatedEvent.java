package com.lethien.common_lib.event;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * SpeakingSubmissionCreatedEvent
 *
 * Published by: Course Service — when a user submits a speaking recording.
 * Consumed by:  AI Service     — speech recognition + pronunciation scoring.
 *
 * Transport: Kafka topic "course.speaking.submission.created"
 *
 * AI Service sau khi chấm xong sẽ publish SubmissionGradedEvent
 * về topic "course.submission.graded" để Course Service cập nhật kết quả.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingSubmissionCreatedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ============================================
    // EVENT METADATA
    // ============================================

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Builder.Default
    private String eventType = "SPEAKING_SUBMISSION_CREATED";

    @Builder.Default
    private ZonedDateTime occurredAt = ZonedDateTime.now();

    // ============================================
    // PAYLOAD
    // ============================================

    private UUID submissionId;
    private UUID userId;
    private UUID exerciseId;
    private Integer attemptNumber;

    /** URL file ghi âm trên S3/CDN — AI Service sẽ download để xử lý */
    private String audioUrl;

    private Integer durationSeconds;

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
                && this.audioUrl != null && !this.audioUrl.isBlank();
    }
}
