package com.lethien.course_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Exercise entity — bài tập thực hành cho 4 kỹ năng (10 loại exercise_type)
 * Maps to: exercises table in course_db
 *
 * JSONB field conventions:
 *   options        : [{"id":"A","text":"..."}] cho MCQ;
 *                    {"left":[...],"right":[...]} cho MATCHING
 *   correct_answer : "A" cho MCQ; ["A","C"] cho multi-select;
 *                    {"pairs":[[0,2],...]} cho MATCHING
 */
@Entity
@Table(name = "exercises", indexes = {
        @Index(name = "idx_exercises_lesson_id", columnList = "lesson_id"),
        @Index(name = "idx_exercises_type",      columnList = "exercise_type"),
        @Index(name = "idx_exercises_order",     columnList = "lesson_id, order_index")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false,
            columnDefinition = "exercise_type")
    private ExerciseType exerciseType;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    /** URL audio câu hỏi (Listening / Speaking exercises) */
    @Column(name = "question_audio_url", columnDefinition = "TEXT")
    private String questionAudioUrl;

    /** Các lựa chọn trả lời dưới dạng JSONB (MCQ, MATCHING, …) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "JSONB")
    private Map<String, Object> options;

    /** Đáp án đúng dưới dạng JSONB */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answer", nullable = false, columnDefinition = "JSONB")
    private Map<String, Object> correctAnswer;

    /** Giải thích hiển thị sau khi user trả lời */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "points", nullable = false)
    @Builder.Default
    private Integer points = 1;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    // ============================================
    // BUSINESS METHODS
    // ============================================

    /**
     * Các loại bài tập cần AI chấm — không thể auto-grade ngay lập tức
     */
    public boolean requiresAiGrading() {
        return this.exerciseType == ExerciseType.ESSAY
                || this.exerciseType == ExerciseType.SPEAKING_RECORD
                || this.exerciseType == ExerciseType.SPEAKING_REPEAT;
    }

    public boolean isSpeakingExercise() {
        return this.exerciseType == ExerciseType.SPEAKING_RECORD
                || this.exerciseType == ExerciseType.SPEAKING_REPEAT;
    }
}
