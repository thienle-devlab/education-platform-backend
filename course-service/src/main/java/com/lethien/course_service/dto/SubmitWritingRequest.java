package com.lethien.course_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * SubmitWritingRequest — request to submit an essay writing.
 * Called via POST /exercises/{exerciseId}/writing
 *
 * Service layer will:
 *   1. Create WritingSubmission with status=PENDING, compute word_count
 *   2. Create ExerciseAttempt with userAnswer={"submission_id":"<uuid>"}
 *   3. Publish event to AI grading queue
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitWritingRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Content is required")
    private String content;

    @Min(value = 0, message = "Time taken must be >= 0")
    private Integer timeTakenSeconds;
}
