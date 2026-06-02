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
 * WritingSubmission entity — bài luận nộp cho bài ESSAY với điểm AI (4 thành phần)
 * Maps to: writing_submissions table in course_db
 *
 * Dùng cho exercise_type: ESSAY.
 * Constraint: (user_id, exercise_id, attempt_number) is UNIQUE.
 *
 * feedback_json format: {"corrections":[...], "suggestions":[...], "highlighted_errors":[...]}
 */
@Entity
@Table(name = "writing_submissions", indexes = {
        @Index(name = "idx_writing_user_id",       columnList = "user_id"),
        @Index(name = "idx_writing_exercise_id",   columnList = "exercise_id"),
        @Index(name = "idx_writing_user_exercise", columnList = "user_id, exercise_id"),
        @Index(name = "idx_writing_status",        columnList = "status"),
        @Index(name = "idx_writing_submitted_at",  columnList = "submitted_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WritingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Soft ref → users.id in User Service (no FK — microservices) */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    /** Nội dung bài viết đầy đủ của user */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "word_count", nullable = false)
    @Builder.Default
    private Integer wordCount = 0;

    // ============================================
    // AI SCORING FIELDS
    // ============================================

    /** Điểm tổng AI (0–100) */
    @Column(name = "ai_score", precision = 5, scale = 2)
    private BigDecimal aiScore;

    /** Điểm ngữ pháp (0–100) */
    @Column(name = "grammar_score", precision = 5, scale = 2)
    private BigDecimal grammarScore;

    /** Điểm từ vựng (0–100) */
    @Column(name = "vocabulary_score", precision = 5, scale = 2)
    private BigDecimal vocabularyScore;

    /** Điểm mạch lạc (0–100) */
    @Column(name = "coherence_score", precision = 5, scale = 2)
    private BigDecimal coherenceScore;

    /** Điểm hoàn thành yêu cầu (0–100) */
    @Column(name = "task_achievement_score", precision = 5, scale = 2)
    private BigDecimal taskAchievementScore;

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

    public void markGraded(BigDecimal aiScore, BigDecimal grammarScore,
                           BigDecimal vocabularyScore, BigDecimal coherenceScore,
                           BigDecimal taskAchievementScore, Map<String, Object> feedbackJson) {
        this.aiScore = aiScore;
        this.grammarScore = grammarScore;
        this.vocabularyScore = vocabularyScore;
        this.coherenceScore = coherenceScore;
        this.taskAchievementScore = taskAchievementScore;
        this.feedbackJson = feedbackJson;
        this.status = SubmissionStatus.GRADED;
        this.gradedAt = ZonedDateTime.now();
    }

    public void markFailed() {
        this.status = SubmissionStatus.FAILED;
        this.gradedAt = ZonedDateTime.now();
    }
}
