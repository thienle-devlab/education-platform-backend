package com.lethien.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * LessonProgress entity — tiến trình từng bài học của mỗi enrollment
 * Maps to: lesson_progress table in course_db
 *
 * Bảng giao giữa enrollments và lessons.
 * Constraint: (enrollment_id, lesson_id) is UNIQUE.
 *
 * NOTE: Khi status thay đổi sang COMPLETED, trigger trigger_progress_update
 * tự động tính lại enrollments.progress_percent.
 */
@Entity
@Table(name = "lesson_progress", indexes = {
        @Index(name = "idx_lesson_progress_enrollment_id",     columnList = "enrollment_id"),
        @Index(name = "idx_lesson_progress_lesson_id",         columnList = "lesson_id"),
        @Index(name = "idx_lesson_progress_status",            columnList = "status"),
        @Index(name = "idx_lesson_progress_enrollment_status", columnList = "enrollment_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "progress_status")
    @Builder.Default
    private  ProgressStatus status = ProgressStatus.NOT_STARTED;

    /** Điểm bài EXERCISE (0–100). NULL cho VIDEO/AUDIO/TEXT. */
    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    /** Tổng thời gian tích lũy user đã ở trên bài học này (giây) */
    @Column(name = "time_spent_seconds", nullable = false)
    @Builder.Default
    private Integer timeSpentSeconds = 0;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

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

    public void markInProgress() {
        if (this.status == ProgressStatus.NOT_STARTED) {
            this.status = ProgressStatus.IN_PROGRESS;
        }
    }

    public void markCompleted(BigDecimal finalScore) {
        this.status = ProgressStatus.COMPLETED;
        this.completedAt = ZonedDateTime.now();
        if (finalScore != null) {
            this.score = finalScore;
        }
    }

    public void addTimeSpent(int seconds) {
        this.timeSpentSeconds += seconds;
    }

}
