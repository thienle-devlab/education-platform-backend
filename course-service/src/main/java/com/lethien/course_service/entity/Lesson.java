package com.lethien.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lesson entity — bài học đơn vị trong một chương
 * Maps to: lessons table in course_db
 *
 * Constraint: (section_id, order_index) is UNIQUE — no duplicate order within a section.
 */
@Entity
@Table(name = "lessons", indexes = {
        @Index(name = "idx_lessons_section_id", columnList = "section_id"),
        @Index(name = "idx_lessons_order",      columnList = "section_id, order_index"),
        @Index(name = "idx_lessons_type",       columnList = "lesson_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, columnDefinition = "lesson_type")
    private LessonType lessonType;

    /** Thứ tự trong chương (0-based). Unique per section — enforced by DB. */
    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    /** Thời lượng nội dung tính bằng giây (VIDEO/AUDIO) */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /** URL nội dung chính (S3 / CDN) */
    @Column(name = "content_url", columnDefinition = "TEXT")
    private String contentUrl;

    /** TRUE = xem được mà không cần enroll */
    @Column(name = "is_preview", nullable = false)
    @Builder.Default
    private Boolean isPreview = false;

    /** TRUE = bài học miễn phí trong khóa có phí */
    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean isFree = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // ============================================
    // RELATIONSHIPS
    // ============================================

    /** 1:1 với skill_content — chỉ có khi lesson có nội dung đặc thù */
    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SkillContent skillContent;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Exercise> exercises = new ArrayList<>();

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

    public boolean isExerciseLesson() {
        return this.lessonType == LessonType.EXERCISE;
    }

    public boolean hasMedia() {
        return this.lessonType == LessonType.VIDEO || this.lessonType == LessonType.AUDIO;
    }
}
