package com.lethien.course_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * CourseTag entity — tags phân loại khóa học (IELTS, TOEIC, Business English, …)
 * Maps to: course_tags table in course_db
 */

@Entity
@Table(name = "course_tags", indexes = {
        @Index(name = "idx_course_tags_course_id", columnList = "course_id"),
        @Index(name = "idx_course_tags_tag_name",  columnList = "tag_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "tag_name", nullable = false, length = 50)
    private String tagName;
}
