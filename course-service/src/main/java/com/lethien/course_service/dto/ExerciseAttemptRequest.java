package com.lethien.course_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * ExerciseAttemptRequest — request to submit an exercise attempt.
 * Called via POST /exercises/{exerciseId}/attempts
 *
 * userAnswer format depends on exercise_type:
 *   MCQ            : {"answer": "A"}
 *   FILL_IN_BLANK  : {"answer": "some text"}
 *   MATCHING       : {"pairs": [[0,2],[1,0],...]}
 *   ORDERING       : {"order": [2,0,1,...]}
 *   ESSAY          : {"submission_id": "<uuid>"}  ← set by service after creating WritingSubmission
 *   SPEAKING_*     : {"submission_id": "<uuid>"}  ← set by service after creating SpeakingSubmission
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseAttemptRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "User answer is required")
    private Map<String, Object> userAnswer;

    @Min(value = 0, message = "Time taken must be >= 0")
    private Integer timeTakenSeconds;
}
