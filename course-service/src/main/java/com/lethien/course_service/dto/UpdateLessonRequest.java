package com.lethien.course_service.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * UpdateLessonRequest — partial update, null fields ignored.
 * lessonType KHÔNG được sửa sau khi tạo để tránh inconsistency với exercises.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLessonRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;
    private Integer durationSeconds;
    private String contentUrl;
    private Boolean isPreview;
    private Boolean isFree;
}
