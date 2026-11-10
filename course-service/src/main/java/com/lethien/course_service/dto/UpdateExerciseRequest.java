package com.lethien.course_service.dto;

import com.lethien.course_service.entity.ExerciseType;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Map;

/**
 * UpdateExerciseRequest — partial update, null fields bị bỏ qua.
 *
 * Nhóm field ảnh hưởng chấm điểm: exerciseType, options, correctAnswer, points.
 * Khi exercise đã có attempt, các field này bị khoá (ContentModificationException)
 * để lịch sử điểm của học viên không bị lệch.
 *
 * Nhóm field luôn sửa được: question, questionAudioUrl, explanation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExerciseRequest {

    // --- luôn sửa được ---
    private String question;
    private String questionAudioUrl;
    private String explanation;

    // --- khoá khi đã có attempt ---
    private ExerciseType exerciseType;
    private Map<String, Object> options;
    private Map<String, Object> correctAnswer;

    @Min(value = 1, message = "Points must be at least 1")
    private Integer points;
}
