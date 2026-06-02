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
 * ExerciseAttempt entity — mỗi lần user nộp bài cho một exercise
 * Maps to: exercise_attempts table in course_db
 *
 * Constraint: (user_id, exercise_id, attempt_number) is UNIQUE.
 *
 * Convention với speaking/writing submissions:
 * Khi exercise_type là ESSAY/SPEAKING_*, tạo đồng thời:
 *   1. ExerciseAttempt     → user_answer = {"submission_id": "<uuid>"}
 *   2. SpeakingSubmission / WritingSubmission → nội dung thực để AI chấm
 * Sau khi AI chấm, score được sync ngược vào ExerciseAttempt.
 */
@Entity
@Table(name = "exercise_attempts", indexes = {
        @Index(name = "idx_exercise_attempts_user_id",       columnList = "user_id"),
        @Index(name = "idx_exercise_attempts_exercise_id",   columnList = "exercise_id"),
        @Index(name = "idx_exercise_attempts_user_exercise", columnList = "user_id, exercise_id"),
        @Index(name = "idx_exercise_attempts_submitted_at",  columnList = "submitted_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    /**
     * Đáp án user nộp dưới dạng JSONB.
     * Với ESSAY/SPEAKING: {"submission_id": "<uuid>"} trỏ sang bảng submissions.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_answer", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> userAnswer;

    /**
     * NULL cho ESSAY và SPEAKING — cần AI chấm.
     * TRUE/FALSE cho các loại auto-grade.
     */
    @Column(name = "is_correct")
    private Boolean isCorrect;

    /** Điểm đạt được. Với ESSAY/SPEAKING: sync từ submission sau khi AI chấm. */
    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

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

    public boolean isPendingAiGrading() {
        return this.isCorrect == null && this.score == null;
    }
}
