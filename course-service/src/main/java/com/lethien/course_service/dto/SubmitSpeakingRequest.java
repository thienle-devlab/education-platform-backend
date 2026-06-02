package com.lethien.course_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * SubmitSpeakingRequest — request to submit a speaking recording.
 * Called via POST /exercises/{exerciseId}/speaking
 *
 * Service layer will:
 *   1. Create SpeakingSubmission with status=PENDING
 *   2. Create ExerciseAttempt with userAnswer={"submission_id":"<uuid>"}
 *   3. Publish event to AI grading queue
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitSpeakingRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Audio URL is required")
    private String audioUrl;

    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;

    @Min(value = 0, message = "Time taken must be >= 0")
    private Integer timeTakenSeconds;
}
