package com.lethien.course_service.dto;

import com.lethien.course_service.entity.ExerciseType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * CreateExerciseRequest — tạo exercise mới trong lesson (lessonType = EXERCISE).
 *
 * Format options / correctAnswer theo exerciseType (được validate ở ExerciseContentValidator):
 *
 *   MULTIPLE_CHOICE   options       : {"choices":[{"id":"A","text":"..."},{"id":"B","text":"..."}]}
 *                     correctAnswer : {"answer":"A"}
 *   TRUE_FALSE        correctAnswer : {"answer":"TRUE"}
 *   SHORT_ANSWER      correctAnswer : {"answer":"..."}
 *   DICTATION         correctAnswer : {"answer":"..."}
 *   FILL_IN_THE_BLANK correctAnswer : {"answers":["word1","word2"]}
 *   MATCHING          options       : {"left":[...],"right":[...]}
 *                     correctAnswer : {"pairs":[[0,2],[1,0]]}
 *   ORDERING          correctAnswer : {"order":[2,0,1]}
 *   ESSAY / SPEAKING_*: correctAnswer không bắt buộc (AI chấm) — để trống sẽ lưu {}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExerciseRequest {

    @NotNull(message = "exerciseType is required")
    private ExerciseType exerciseType;

    @NotBlank(message = "Question is required")
    private String question;

    private String questionAudioUrl;

    private Map<String, Object> options;

    private Map<String, Object> correctAnswer;

    private String explanation;

    @Min(value = 1, message = "Points must be at least 1")
    @Builder.Default
    private Integer points = 1;
}
