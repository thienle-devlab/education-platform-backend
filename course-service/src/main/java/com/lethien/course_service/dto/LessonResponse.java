package com.lethien.course_service.dto;

import com.lethien.course_service.entity.Lesson;
import com.lethien.course_service.entity.LessonType;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

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
    private String contentUrl;
    private Boolean isPreview;
    private Boolean isFree;

    // null nếu lesson chưa có SkillContent
    private SkillContentResponse skillContent;

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static LessonResponse from(Lesson lesson) {
        SkillContentResponse skillContentResponse = null;
        if (lesson.getSkillContent() != null) {
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
                .contentUrl(lesson.getContentUrl())
                .isPreview(lesson.getIsPreview())
                .isFree(lesson.getIsFree())
                .skillContent(skillContentResponse)
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }
}
