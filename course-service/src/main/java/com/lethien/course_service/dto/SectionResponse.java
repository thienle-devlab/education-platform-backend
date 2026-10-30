package com.lethien.course_service.dto;

import com.lethien.course_service.entity.Section;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * SectionResponse — trả về thông tin section.
 * Không bao gồm lessons — dùng GET /courses/{id}/curriculum để lấy full.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionResponse {

    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private Integer orderIndex;
    private Integer totalLessons;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static SectionResponse from(Section section) {
        return SectionResponse.builder()
                .id(section.getId())
                .courseId(section.getCourse().getId())
                .title(section.getTitle())
                .description(section.getDescription())
                .orderIndex(section.getOrderIndex())
                .totalLessons(section.getLessons().size())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }
}
