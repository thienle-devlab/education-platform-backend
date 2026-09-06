package com.lethien.common_lib.event;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * SubmissionGradedEvent
 *
 * Published by: AI Service    — after grading a speaking or writing submission.
 * Consumed by:  Course Service — update submission status, sync score to ExerciseAttempt.
 *
 * Transport: Kafka topic "course.submission.graded"
 *
 * submissionType field dùng để Course Service route đúng:
 *   "SPEAKING" → update SpeakingSubmission
 *   "WRITING"  → update WritingSubmission
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionGradedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ============================================
    // EVENT METADATA
    // ============================================

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Builder.Default
    private String eventType = "SUBMISSION_GRADED";

    @Builder.Default
    private ZonedDateTime occurredAt = ZonedDateTime.now();

    // ============================================
    // ROUTING
    // ============================================

    /** "SPEAKING" hoặc "WRITING" — Course Service dùng để route đúng bảng */
    private String submissionType;

    private UUID submissionId;
    private UUID exerciseId;
    private UUID userId;

    // ============================================
    // GRADING RESULT
    // ============================================

    /** true = graded thành công, false = AI failed */
    private Boolean success;

    /** Điểm tổng (0–100) */
    private BigDecimal aiScore;

    /**
     * Điểm thành phần theo submissionType:
     *   SPEAKING: { "fluency": 85.0, "pronunciation": 78.5 }
     *   WRITING:  { "grammar": 80.0, "vocabulary": 75.0, "coherence": 70.0, "taskAchievement": 85.0 }
     */
    private Map<String, BigDecimal> componentScores;

    /**
     * Phản hồi chi tiết từ AI dưới dạng JSON string.
     * Course Service parse và lưu vào feedback_json JSONB.
     */
    private Map<String, Object> feedbackJson;

    /** Lý do thất bại nếu success=false */
    private String failureReason;

    private ZonedDateTime gradedAt;

    // ============================================
    // BUSINESS METHODS
    // ============================================

    public String idempotencyKey() {
        return this.eventType + ":" + this.submissionId;
    }

    public boolean isValid() {
        return this.submissionId != null
                && this.submissionType != null
                && this.success != null;
    }

    public boolean isSpeaking() {
        return "SPEAKING".equalsIgnoreCase(this.submissionType);
    }

    public boolean isWriting() {
        return "WRITING".equalsIgnoreCase(this.submissionType);
    }
}
