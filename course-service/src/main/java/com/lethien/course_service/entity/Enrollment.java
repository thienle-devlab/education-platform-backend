package com.lethien.course_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Enrollment entity — theo dõi việc đăng ký khóa học của user
 * Maps to: enrollments table in course_db
 *
 * Constraint: (user_id, course_id) is UNIQUE — mỗi user chỉ enroll 1 lần.
 *
 * NOTE: progress_percent được tự động cập nhật bởi trigger trigger_progress_update
 * khi lesson_progress thay đổi — không cập nhật thủ công.
 */
@Entity
@Table(name = "enrollments", indexes = {
        @Index(name = "idx_enrollments_user_id",       columnList = "user_id"),
        @Index(name = "idx_enrollments_course_id",     columnList = "course_id"),
        @Index(name = "idx_enrollments_status",        columnList = "status"),
        @Index(name = "idx_enrollments_user_course",   columnList = "user_id, course_id"),
        @Index(name = "idx_enrollments_last_accessed", columnList = "last_accessed_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Soft ref → users.id in User Service (no FK — microservices) */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false,
            columnDefinition = "enrollment_status")
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    /**
     * Tỷ lệ hoàn thành khóa học (0.00–100.00).
     * Tự động cập nhật bởi trigger trigger_progress_update.
     */
    @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal progressPercent = BigDecimal.ZERO;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private ZonedDateTime enrolledAt;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    @Column(name = "expired_at")
    private ZonedDateTime expiredAt;

    @Column(name = "last_accessed_at")
    private ZonedDateTime lastAccessedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // ============================================
    // RELATIONSHIPS
    // ============================================

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LessonProgress> lessonProgresses = new ArrayList<>();

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        this.enrolledAt = now;
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

    public boolean isActive() {
        return this.status == EnrollmentStatus.ACTIVE;
    }

    public boolean isCompleted() {
        return this.status == EnrollmentStatus.COMPLETED;
    }

    public void recordAccess() {
        this.lastAccessedAt = ZonedDateTime.now();
    }
}
