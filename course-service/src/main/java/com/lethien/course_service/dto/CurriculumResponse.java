package com.lethien.course_service.dto;

import com.lethien.course_service.entity.Section;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * CurriculumResponse — toàn bộ cấu trúc course.
 * Used in: GET /courses/{id}/curriculum
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumResponse {

    private UUID courseId;
    private String courseTitle;
    private Integer totalSections;
    private Integer totalLessons;
    private List<SectionWithLessons> sections;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SectionWithLessons {
        private UUID id;
        private String title;
        private String description;
        private Integer orderIndex;
        private Integer totalLessons;
        private List<LessonResponse> lessons;

        public static SectionWithLessons from(Section section) {
            List<LessonResponse> lessons = section.getLessons().stream()
                    .map(LessonResponse::from)
                    .toList();

            return SectionWithLessons.builder()
                    .id(section.getId())
                    .title(section.getTitle())
                    .description(section.getDescription())
                    .orderIndex(section.getOrderIndex())
                    .totalLessons(lessons.size())
                    .lessons(lessons)
                    .build();
        }
    }
}
