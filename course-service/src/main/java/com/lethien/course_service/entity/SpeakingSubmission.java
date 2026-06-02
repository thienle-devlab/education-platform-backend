package com.lethien.course_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * SpeakingSubmission entity — bản ghi âm nộp cho bài SPEAKING với điểm AI
 * Maps to: speaking_submissions table in course_db
 *
 * Dùng cho exercise_type: SPEAKING_RECORD, SPEAKING_REPEAT.
 * Constraint: (user_id, exercise_id, attempt_number) is UNIQUE.
 *
 * feedback_json format: {"phonemes":[...], "suggestions":[...], "highlights":[...]}
 */
@Entity
@Table(name = "speaking_submissions", indexes = {
        @Index(name = "idx_speaking_user_id",       columnList = "user_id"),
        @Index(name = "idx_speaking_exercise_id",   columnList = "exercise_id"),
        @Index(name = "idx_speaking_user_exercise", columnList = "user_id, exercise_id"),
        @Index(name = "idx_speaking_status",        columnList = "status"),
        @Index(name = "idx_speaking_submitted_at",  columnList = "submitted_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    /** URL file ghi âm trên S3/CDN */
    @Column(name = "audio_url", nullable = false, columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // ============================================
    // AI SCORING FIELDS
    // ============================================

    /** Điểm tổng AI (0–100) */
    @Column(name = "ai_score", precision = 5, scale = 2)
    private BigDecimal aiScore;

    /** Điểm trôi chảy (0–100) */
    @Column(name = "fluency_score", precision = 5, scale = 2)
    private BigDecimal fluencyScore;

    /** Độ chính xác phát âm (0–100) */
    @Column(name = "pronunciation_score", precision = 5, scale = 2)
    private BigDecimal pronunciationScore;

    /** Phản hồi chi tiết AI dưới dạng JSONB */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feedback_json", columnDefinition = "JSONB")
    private Map<String, Object> feedbackJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "submission_status")
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "graded_at")
    private ZonedDateTime gradedAt;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private ZonedDateTime submittedAt;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.submittedAt = ZonedDateTime.now();
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    public boolean isGraded() {
        return this.status == SubmissionStatus.GRADED;
    }

    public void markGraded(BigDecimal aiScore, BigDecimal fluencyScore,
                           BigDecimal pronunciationScore, Map<String, Object> feedbackJson) {
        this.aiScore = aiScore;
        this.fluencyScore = fluencyScore;
        this.pronunciationScore = pronunciationScore;
        this.feedbackJson = feedbackJson;
        this.status = SubmissionStatus.GRADED;
        this.gradedAt = ZonedDateTime.now();
    }

    public void markFailed() {
        this.status = SubmissionStatus.FAILED;
        this.gradedAt = ZonedDateTime.now();
    }
}
