package com.lethien.course_service.dto;

import com.lethien.course_service.entity.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * CourseResponse — full course detail returned to authenticated users.
 * Used in: GET /courses/{id}, GET /courses/slug/{slug}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private UUID id;
    private UUID instructorId;

    private String title;
    private String slug;
    private String description;

    private SkillType skillType;
    private CourseLevel level;
    private CourseStatus status;

    private String thumbnailUrl;
    private String trailerUrl;
    private BigDecimal price;
    private boolean isFree;

    private Integer durationHours;
    private Integer totalLessons;
    private Integer totalEnrollments;
    private BigDecimal averageRating;

    private Set<String> tags;

    private ZonedDateTime publishedAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    // ============================================
    // MAPPER
    // ============================================

    public static CourseResponse from(Course course) {
        Set<String> tagNames = course.getTags().stream()
                .map(CourseTag::getTagName)
                .collect(Collectors.toSet());

        return CourseResponse.builder()
                .id(course.getId())
                .instructorId(course.getInstructorId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .description(course.getDescription())
                .skillType(course.getSkillType())
                .level(course.getLevel())
                .status(course.getStatus())
                .thumbnailUrl(course.getThumbnailUrl())
                .trailerUrl(course.getTrailerUrl())
                .price(course.getPrice())
                .isFree(course.isFree())
                .durationHours(course.getDurationHours())
                .totalLessons(course.getTotalLessons())
                .totalEnrollments(course.getTotalEnrollments())
                .averageRating(course.getAverageRating())
                .tags(tagNames)
                .publishedAt(course.getPublishedAt())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
