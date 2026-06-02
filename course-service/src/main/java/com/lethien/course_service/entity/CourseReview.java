package com.lethien.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * CourseReview entity — đánh giá và nhận xét khóa học
 * Maps to: course_reviews table in course_db
 *
 * Constraint: (user_id, course_id) is UNIQUE — mỗi user chỉ review 1 lần.
 *
 * NOTE: Khi review thay đổi, trigger trigger_rating_update tự động
 * cập nhật courses.average_rating.
 */
@Entity
@Table(name = "course_reviews", indexes = {
        @Index(name = "idx_reviews_course_id",  columnList = "course_id"),
        @Index(name = "idx_reviews_user_id",    columnList = "user_id"),
        @Index(name = "idx_reviews_rating",     columnList = "course_id, rating"),
        @Index(name = "idx_reviews_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReview {

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

    /** Số sao: 1–5. Trigger cập nhật courses.average_rating sau khi lưu. */
    @Column(name = "rating", nullable = false)
    private Short rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /**
     * TRUE nếu user đang actively enrolled — kiểm tra ở application layer.
     */
    @Column(name = "is_verified_purchase", nullable = false)
    @Builder.Default
    private Boolean isVerifiedPurchase = false;

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
}
