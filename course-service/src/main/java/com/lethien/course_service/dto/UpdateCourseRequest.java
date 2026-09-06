package com.lethien.course_service.dto;

import com.lethien.course_service.entity.CourseLevel;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * UpdateCourseRequest — partial update payload for PATCH /courses/{id}.
 * All fields are optional — null means "keep existing value".
 *
 * instructorId is required to verify ownership server-side.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCourseRequest {

    @NotNull(message = "Instructor ID is required for ownership verification")
    private UUID instructorId;

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    private CourseLevel level;

    private String thumbnailUrl;

    private String trailerUrl;

    @DecimalMin(value = "0", message = "Price must be >= 0")
    private BigDecimal price;

    @Min(value = 1, message = "Duration must be at least 1 hour")
    private Integer durationHours;

    @Size(max = 10, message = "A course can have at most 10 tags")
    private Set<@NotBlank @Size(max = 50) String> tags;

}
