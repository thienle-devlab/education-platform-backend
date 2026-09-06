package com.lethien.course_service.dto;

import com.lethien.course_service.entity.ExerciseAttempt;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * ExerciseAttemptResponse — result returned after submitting an exercise attempt.
 * Used in: POST /exercises/{id}/attempts
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseAttemptResponse {

    private UUID id;
    private UUID exerciseId;
    private UUID userId;

    private Integer attemptNumber;
    private Map<String, Object> userAnswer;

    // null cho ESSAY / SPEAKING — chờ AI chấm
    private Boolean isCorrect;
    private BigDecimal score;

    private Integer timeTakenSeconds;
    private ZonedDateTime submittedAt;

    // Chỉ có khi isCorrect != null (auto-graded)
    private String explanation;

    // ============================================
    // MAPPER
    // ============================================

    public static ExerciseAttemptResponse from(ExerciseAttempt attempt) {
        return ExerciseAttemptResponse.builder()
                .id(attempt.getId())
                .exerciseId(attempt.getExercise().getId())
                .userId(attempt.getUserId())
                .attemptNumber(attempt.getAttemptNumber())
                .userAnswer(attempt.getUserAnswer())
                .isCorrect(attempt.getIsCorrect())
                .score(attempt.getScore())
                .timeTakenSeconds(attempt.getTimeTakenSeconds())
                .submittedAt(attempt.getSubmittedAt())
                .explanation(attempt.getIsCorrect() != null
                        ? attempt.getExercise().getExplanation()
                        : null)
                .build();
    }
}
