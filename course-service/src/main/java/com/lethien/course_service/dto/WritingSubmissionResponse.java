package com.lethien.course_service.dto;

import com.lethien.course_service.entity.SubmissionStatus;
import com.lethien.course_service.entity.WritingSubmission;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * WritingSubmissionResponse — returned immediately after submit with status=PENDING.
 * AI scores null until graded.
 * Client polls GET /exercises/{id}/writing/{submissionId} to get final result.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WritingSubmissionResponse {

    private UUID id;
    private UUID exerciseId;
    private UUID userId;
    private Integer attemptNumber;
    private String content;
    private Integer wordCount;

    // null khi status = PENDING / PROCESSING
    private BigDecimal aiScore;
    private BigDecimal grammarScore;
    private BigDecimal vocabularyScore;
    private BigDecimal coherenceScore;
    private BigDecimal taskAchievementScore;
    private Map<String, Object> feedbackJson;

    private SubmissionStatus status;
    private ZonedDateTime gradedAt;
    private ZonedDateTime submittedAt;

    public static WritingSubmissionResponse from(WritingSubmission s) {
        return WritingSubmissionResponse.builder()
                .id(s.getId())
                .exerciseId(s.getExercise().getId())
                .userId(s.getUserId())
                .attemptNumber(s.getAttemptNumber())
                .content(s.getContent())
                .wordCount(s.getWordCount())
                .aiScore(s.getAiScore())
                .grammarScore(s.getGrammarScore())
                .vocabularyScore(s.getVocabularyScore())
                .coherenceScore(s.getCoherenceScore())
                .taskAchievementScore(s.getTaskAchievementScore())
                .feedbackJson(s.getFeedbackJson())
                .status(s.getStatus())
                .gradedAt(s.getGradedAt())
                .submittedAt(s.getSubmittedAt())
                .build();
    }
}
