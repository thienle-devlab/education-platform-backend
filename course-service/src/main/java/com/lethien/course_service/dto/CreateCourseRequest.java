package com.lethien.course_service.dto;

import com.lethien.course_service.entity.CourseLevel;
import com.lethien.course_service.entity.SkillType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * CreateCourseRequest — request to create a new course.
 * Called by instructor via POST /courses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseRequest {

    @NotNull(message = "Instructor ID is required")
    private UUID instructorId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Slug is required")
    @Size(max = 255, message = "Slug must not exceed 255 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug must be lowercase alphanumeric with hyphens (e.g. english-listening-beginner)")
    private String slug;

    private String description;

    @NotNull(message = "Skill type is required")
    private SkillType skillType;

    private CourseLevel level = CourseLevel.BEGINNER;

    private String thumbnailUrl;
    private String trailerUrl;

    @DecimalMin(value = "0", message = "Price must be >= 0")
    private BigDecimal price = BigDecimal.ZERO;

    @Min(value = 1, message = "Duration must be at least 1 hour")
    private Integer durationHours;

    @Size(max = 10, message = "A course can have at most 10 tags")
    private Set<@NotBlank @Size(max = 50) String> tags;
}
