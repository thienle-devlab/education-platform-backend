package com.lethien.course_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * SkillContent entity — metadata nội dung đặc thù theo kỹ năng
 * Maps to: skill_content table in course_db
 *
 * Quan hệ 1:1 với Lesson (UNIQUE lesson_id — enforced by DB).
 *
 * Cấu trúc content_data JSONB theo skill_type:
 *   LISTENING : { "audio_url": "...", "transcript": "...", "accent": "British|American" }
 *   SPEAKING  : { "sample_audio_url": "...", "phoneme_targets": [...], "ipa_text": "..." }
 *   READING   : { "article_text": "...", "word_count": 500, "source_url": "..." }
 *   WRITING   : { "prompt": "...", "min_words": 200, "writing_type": "essay|email|report" }
 */
@Entity
@Table(name = "skill_content", indexes = {
        @Index(name = "idx_skill_content_lesson_id",  columnList = "lesson_id"),
        @Index(name = "idx_skill_content_skill_type", columnList = "skill_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "skill_type", nullable = false,
            columnDefinition = "skill_type")
    private SkillType skillType;

    /**
     * Dữ liệu có cấu trúc đặc thù theo skill_type.
     * Sử dụng Map<String, Object> để giữ tính linh hoạt với nhiều schema JSONB.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_data", nullable = false, columnDefinition = "JSONB")
    @Builder.Default
    private Map<String, Object> contentData = Map.of();

    /** File media chính (audio/video URL) */
    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    /** Transcript đầy đủ cho bài LISTENING và SPEAKING */
    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

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
