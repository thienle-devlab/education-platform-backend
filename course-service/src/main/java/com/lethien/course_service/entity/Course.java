package com.lethien.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Course entity — stores English course information
 * Maps to: courses table in course_db
 *
 * NOTE: total_lessons, total_enrollments, average_rating are denormalized fields
 * managed by PostgreSQL triggers — do NOT update them directly from application code.
 */

@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_courses_instructor_id", columnList = "instructor_id"),
        @Index(name = "idx_courses_skill_type",    columnList = "skill_type"),
        @Index(name = "idx_courses_level",         columnList = "level"),
        @Index(name = "idx_courses_status",        columnList = "status"),
        @Index(name = "idx_courses_slug",          columnList = "slug"),
        @Index(name = "idx_courses_published_at",  columnList = "published_at DESC"),
        @Index(name = "idx_courses_price",         columnList = "price"),
        @Index(name = "idx_courses_skill_status",  columnList = "skill_type, status"),
        @Index(name = "idx_courses_level_status",  columnList = "level, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Soft ref → users.id in User Service (no FK — microservices) */
    @Column(name = "instructor_id", nullable = false)
    private UUID instructorId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** SEO-friendly URL slug — must be globally unique */
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, columnDefinition = "TEXT")
    private SkillType skillType;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false,
            columnDefinition = "course_level")
    @Builder.Default
    private CourseLevel level = CourseLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,
            columnDefinition = "course_status")
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "trailer_url", columnDefinition = "TEXT")
    private String trailerUrl;

    /** Course price in VND. 0 = free */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "duration_hours")
    private Integer durationHours;

    // ============================================
    // DENORMALIZED FIELDS — managed by DB triggers
    // ============================================
    @Column(name = "total_lessons", nullable = false)
    @Builder.Default
    private Integer totalLessons = 0;

    @Column(name = "total_enrollments", nullable = false)
    @Builder.Default
    private Integer totalEnrollments = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "published_at")
    private ZonedDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // ============================================
    // RELATIONSHIPS
    // ============================================
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseTag> tags = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Section> sections = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CourseReview> reviews = new ArrayList<>();

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

    public void publish() {
        this.status = CourseStatus.PUBLISHED;
        this.publishedAt = ZonedDateTime.now();
    }

    public void archive() {
        this.status = CourseStatus.ARCHIVED;
    }

    public boolean isPublished() {
        return this.status == CourseStatus.PUBLISHED;
    }

    public boolean isFree() {
        return this.price.compareTo(BigDecimal.ZERO) == 0;
    }

    public void addTag(String tagName) {
        CourseTag tag = CourseTag.builder()
                .course(this)
                .tagName(tagName)
                .build();
        this.tags.add(tag);
    }

    public void removeTag(String tagName) {
        this.tags.removeIf(t -> t.getTagName().equalsIgnoreCase(tagName));
    }
}
