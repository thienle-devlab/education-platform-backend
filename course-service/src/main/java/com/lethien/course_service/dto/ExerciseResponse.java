package com.lethien.course_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lethien.course_service.entity.Exercise;
import com.lethien.course_service.entity.ExerciseType;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * ExerciseResponse — một DTO, hai cách map:
 *
 *   forLearner(e) → KHÔNG có correctAnswer, explanation (học viên chỉ thấy explanation
 *                   sau khi nộp bài, qua ExerciseAttemptResponse)
 *   forOwner(e)   → đầy đủ, chỉ dành cho INSTRUCTOR sở hữu course
 *
 * @JsonInclude(NON_NULL): field null sẽ không xuất hiện trong JSON,
 * nên bản của học viên không lộ cả tên field đáp án.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExerciseResponse {

    private UUID id;
    private UUID lessonId;
    private ExerciseType exerciseType;
    private String question;
    private String questionAudioUrl;
    private Map<String, Object> options;
    private Integer points;
    private Integer orderIndex;

    // --- chỉ owner ---
    private Map<String, Object> correctAnswer;
    private String explanation;

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public static ExerciseResponse forLearner(Exercise e) {
        return base(e).build();
    }

    public static ExerciseResponse forOwner(Exercise e) {
        return base(e)
                .correctAnswer(e.getCorrectAnswer())
                .explanation(e.getExplanation())
                .build();
    }

    public static ExerciseResponseBuilder base(Exercise e) {
        return ExerciseResponse.builder()
                .id(e.getId())
                .lessonId(e.getLesson().getId())
                .exerciseType(e.getExerciseType())
                .question(e.getQuestion())
                .questionAudioUrl(e.getQuestionAudioUrl())
                .options(e.getOptions())
                .points(e.getPoints())
                .orderIndex(e.getOrderIndex())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt());
    }
}
