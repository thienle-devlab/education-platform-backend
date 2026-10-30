package com.lethien.course_service.dto;

import com.lethien.course_service.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * CreateLessonRequest — tạo lesson mới trong section.
 *
 * skillContent là optional — truyền khi đã có dữ liệu kỹ năng.
 * Nếu chưa có, có thể tạo sau qua PATCH /lessons/{id}/skill-content.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLessonRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "lessonType is required")
    private LessonType lessonType;

    private Integer durationSeconds;
    private String contentUrl;

    private Boolean isPreview = false;
    private Boolean isFree = false;

    /**
     * Optional — truyền khi muốn tạo SkillContent cùng lúc với Lesson.
     * Chỉ có ý nghĩa với lessonType = VIDEO / AUDIO / TEXT.
     */
    private SkillContentRequest skillContent;
}
