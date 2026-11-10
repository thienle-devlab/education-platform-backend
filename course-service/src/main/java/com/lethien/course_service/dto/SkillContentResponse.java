package com.lethien.course_service.dto;

import com.lethien.course_service.entity.SkillContent;
import com.lethien.course_service.entity.SkillType;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillContentResponse {

    private UUID id;
    private UUID lessonId;
    private SkillType skillType;
    private String mediaUrl;
    private String thumbnailUrl;
    private String transcript;
    private Map<String, Object> contentData;

    public static SkillContentResponse from(SkillContent sc) {
        return SkillContentResponse.builder()
                .id(sc.getId())
                .lessonId(sc.getLesson().getId())
                .skillType(sc.getSkillType())
                .mediaUrl(sc.getMediaUrl())
                .thumbnailUrl(sc.getThumbnailUrl())
                .transcript(sc.getTranscript())
                .contentData(sc.getContentData())
                .build();
    }
}
