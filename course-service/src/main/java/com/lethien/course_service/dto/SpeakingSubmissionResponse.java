package com.lethien.course_service.dto;

import com.lethien.course_service.entity.SpeakingSubmission;
import com.lethien.course_service.entity.SubmissionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * SpeakingSubmissionResponse — returned immediately after submit with status=PENDING.
 * AI scores null until graded.
 * Client polls GET /exercises/{id}/speaking/{submissionId} to get final result.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingSubmissionResponse {

    private UUID id;
    private UUID exerciseId;
    private UUID userId;
    private Integer attemptNumber;
    private String audioUrl;
    private Integer durationSeconds;

    // null khi status = PENDING / PROCESSING
    private BigDecimal aiScore;
    private BigDecimal fluencyScore;
    private BigDecimal pronunciationScore;
    private Map<String, Object> feedbackJson;

    private SubmissionStatus status;
    private ZonedDateTime gradedAt;
    private ZonedDateTime submittedAt;

    public static SpeakingSubmissionResponse from(SpeakingSubmission s) {
        return SpeakingSubmissionResponse.builder()
                .id(s.getId())
                .exerciseId(s.getExercise().getId())
                .userId(s.getUserId())
                .attemptNumber(s.getAttemptNumber())
                .audioUrl(s.getAudioUrl())
                .durationSeconds(s.getDurationSeconds())
                .aiScore(s.getAiScore())
                .fluencyScore(s.getFluencyScore())
                .pronunciationScore(s.getPronunciationScore())
                .feedbackJson(s.getFeedbackJson())
                .status(s.getStatus())
                .gradedAt(s.getGradedAt())
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}
