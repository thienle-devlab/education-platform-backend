package com.lethien.course_service.dto;

import com.lethien.course_service.entity.Course;
import com.lethien.course_service.entity.CourseLevel;
import com.lethien.course_service.entity.CourseTag;
import com.lethien.course_service.entity.SkillType;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CourseSummaryResponse — lightweight card used in catalog / search results.
 * Used in: GET /courses (paginated list), search endpoints, home page.
 *
 * Intentionally excludes: description, trailerUrl, sections — kept slim for lists.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSummaryResponse {

    private UUID id;
    private UUID instructorId;

    private String title;
    private String slug;
    private String thumbnailUrl;

    private SkillType skillType;
    private CourseLevel level;

    private BigDecimal price;
    private boolean isFree;

    private Integer totalLessons;
    private Integer totalEnrollments;
    private BigDecimal averageRating;

    private Set<String> tags;

    // ============================================
    // MAPPER
    // ============================================

    public static CourseSummaryResponse from(Course course) {
        Set<String> tagNames = course.getTags().stream()
                .map(CourseTag::getTagName)
                .collect(Collectors.toSet());

        return CourseSummaryResponse.builder()
                .id(course.getId())
                .instructorId(course.getInstructorId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .thumbnailUrl(course.getThumbnailUrl())
                .skillType(course.getSkillType())
                .level(course.getLevel())
                .price(course.getPrice())
                .isFree(course.isFree())
                .totalLessons(course.getTotalLessons())
                .totalEnrollments(course.getTotalEnrollments())
                .averageRating(course.getAverageRating())
                .tags(tagNames)
                .build();
    }
}
