package com.lethien.course_service.dto;

import com.lethien.course_service.entity.SkillType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * SkillContentRequest — metadata đặc thù theo kỹ năng.
 * Tạo cùng lúc với Lesson (luồng chính) hoặc riêng qua PATCH /lessons/{id}/skill-content.
 *
 * contentData JSONB theo skillType:
 *   LISTENING : { "accent": "British", "transcript": "..." }
 *   SPEAKING  : { "phoneme_targets": [...], "ipa_text": "..." }
 *   READING   : { "word_count": 500, "source_url": "..." }
 *   WRITING   : { "min_words": 200, "writing_type": "essay" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillContentRequest {
        @NotNull(message = "skillType is required")
        private SkillType skillType;

        private String mediaUrl;
        private String thumbnailUrl;
        private String transcript;

        private Map<String, Object> contentData;
}
