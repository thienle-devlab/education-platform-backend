package com.lethien.course_service.dto;

import com.lethien.course_service.entity.Lesson;
import com.lethien.course_service.entity.LessonType;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * LessonResponse — một DTO, hai chế độ:
 *
 *   from(lesson)                  → đầy đủ (dùng cho instructor khi tạo/sửa lesson)
 *   from(lesson, includeContent)  → includeContent = false thì ẩn contentUrl và skillContent
 *                                   (mediaUrl, transcript, contentData) và đặt contentLocked = true
 *
 * contentLocked cho frontend biết hiển thị biểu tượng khóa thay vì coi như lesson trống.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {

    private UUID id;
    private UUID sectionId;
    private String title;
    private String description;
    private LessonType lessonType;
    private Integer orderIndex;
    private Integer durationSeconds;
    private Boolean isPreview;
    private Boolean isFree;

    // --- nội dung: null khi bị khóa ---
    private String contentUrl;
    private SkillContentResponse skillContent;

    private Boolean contentLocked;

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static LessonResponse from(Lesson lesson) {
        return from(lesson, true);
    }

    public static LessonResponse from(Lesson lesson, boolean includeContent) {
        // Khi khóa, không đụng tới lesson.getSkillContent() để khỏi phát sinh query thừa
        SkillContentResponse skillContentResponse = null;
        if (includeContent && lesson.getSkillContent() != null) {
            skillContentResponse = SkillContentResponse.from(lesson.getSkillContent());
        }

        return LessonResponse.builder()
                .id(lesson.getId())
                .sectionId(lesson.getSection().getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .lessonType(lesson.getLessonType())
                .orderIndex(lesson.getOrderIndex())
                .durationSeconds(lesson.getDurationSeconds())
                .isPreview(lesson.getIsPreview())
                .isFree(lesson.getIsFree())
                .contentUrl(includeContent ? lesson.getContentUrl() : null)
                .skillContent(skillContentResponse)
                .contentLocked(!includeContent)
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }
}
