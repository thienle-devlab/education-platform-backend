package com.lethien.course_service.dto;

import com.lethien.course_service.entity.Section;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * CurriculumResponse — toàn bộ cấu trúc course.
 * Used in: GET /courses/{id}/curriculum
 *
 * viewerAccess (OWNER | ENROLLED | PUBLIC) cho frontend biết người xem đang ở mức nào,
 * ví dụ để hiện nút "Đăng ký học" khi là PUBLIC.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumResponse {

    private UUID courseId;
    private String courseTitle;
    private String viewerAccess;
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

        /**
         * @param fullAccess true (OWNER/ENROLLED) → mọi lesson đầy đủ nội dung;
         *                   false (PUBLIC) → chỉ lesson isPreview mới có nội dung.
         */

        public static SectionWithLessons from(Section section, boolean fullAccess) {
            List<LessonResponse> lessons = section.getLessons().stream()
                    .map(l -> LessonResponse.from(l,
                            fullAccess || Boolean.TRUE.equals(l.getIsPreview())))
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
