package com.lethien.course_service.service;

import com.lethien.common_lib.constant.KafkaTopics;
import com.lethien.common_lib.event.SpeakingSubmissionCreatedEvent;
import com.lethien.common_lib.event.SubmissionGradedEvent;
import com.lethien.common_lib.event.WritingSubmissionCreatedEvent;
import com.lethien.course_service.dto.SpeakingSubmissionResponse;
import com.lethien.course_service.dto.SubmitSpeakingRequest;
import com.lethien.course_service.dto.SubmitWritingRequest;
import com.lethien.course_service.dto.WritingSubmissionResponse;
import com.lethien.course_service.entity.*;
import com.lethien.course_service.exception.NotEnrolledException;
import com.lethien.course_service.exception.SubmissionNotFoundException;
import com.lethien.course_service.repository.EnrollmentRepository;
import com.lethien.course_service.repository.ExerciseAttemptRepository;
import com.lethien.course_service.repository.SpeakingSubmissionRepository;
import com.lethien.course_service.repository.WritingSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * SubmissionService — handles ESSAY, SPEAKING_RECORD, SPEAKING_REPEAT submissions.
 *
 * Flow for Speaking/Writing:
 *   1. Validate access (enrolled or preview)
 *   2. Resolve next attempt number
 *   3. Create SpeakingSubmission / WritingSubmission with status=PENDING
 *   4. Create ExerciseAttempt with userAnswer={"submission_id":"<uuid>"}
 *   5. Publish event to Kafka → AI Service grades async
 *   6. Return full submission object with status=PENDING
 *
 * Flow when AI grading completes (consume SubmissionGradedEvent):
 *   1. Update SpeakingSubmission / WritingSubmission with scores
 *   2. Sync score to ExerciseAttempt via ExerciseService.syncAiScore()
 *   3. LessonProgress auto-updates via ExerciseService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {
    private final SpeakingSubmissionRepository speakingRepository;
    private final WritingSubmissionRepository writingRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExerciseService exerciseService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ============================================
    // SUBMIT SPEAKING
    // ============================================

    /**
     * Submit a speaking recording for AI grading.
     * Returns full SpeakingSubmission with status=PENDING immediately.
     *
     * @param exerciseId target exercise (must be SPEAKING_RECORD or SPEAKING_REPEAT)
     * @param request    audio URL + duration payload
     */
    @Transactional
    public SpeakingSubmissionResponse submitSpeaking(UUID exerciseId, SubmitSpeakingRequest request) {

        log.info("Submitting speaking: exerciseId={}, userId={}", exerciseId, request.getUserId());

        // 1️⃣ Load and validate exercise type
        Exercise exercise = exerciseService.findExerciseById(exerciseId);
        if (!exercise.isSpeakingExercise()) {
            throw new IllegalArgumentException("Exercise is not a speaking type: " + exercise.getExerciseType());
        }

        // 2️⃣ Validate access
        validateAccess(exercise.getLesson(), request.getUserId());

        // 3️⃣ Next attempt number
        int attemptNumber = attemptRepository.findNextAttemptNumber(request.getUserId(), exerciseId);

        // 4️⃣ Create SpeakingSubmission with status=PENDING
        SpeakingSubmission submission = SpeakingSubmission.builder()
                .userId(request.getUserId())
                .exercise(exercise)
                .attemptNumber(attemptNumber)
                .audioUrl(request.getAudioUrl())
                .durationSeconds(request.getDurationSeconds())
                .status(SubmissionStatus.PENDING)
                .build();

        SpeakingSubmission saved = speakingRepository.save(submission);

        log.info("SpeakingSubmission created: submissionId={}, attemptNumber={}",
                saved.getId(), attemptNumber);

        // 5️⃣ Create ExerciseAttempt — userAnswer trỏ sang submissionId
        createLinkedAttempt(request.getUserId(), exercise, attemptNumber, saved.getId(), request.getTimeTakenSeconds());

        // 6️⃣ Publish event → AI Service
        publishSpeakingEvent(saved, exercise);

        return SpeakingSubmissionResponse.from(saved);
    }

    // ============================================
    // SUBMIT WRITING
    // ============================================

    /**
     * Submit an essay for AI grading.
     * Returns full WritingSubmission with status=PENDING immediately.
     *
     * @param exerciseId target exercise (must be ESSAY)
     * @param request    essay content payload
     */
    @Transactional
    public WritingSubmissionResponse submitWriting(UUID exerciseId, SubmitWritingRequest request) {

        log.info("Submitting writing: exerciseId={}, userId={}", exerciseId, request.getUserId());

        // 1️⃣ Load and validate exercise type
        Exercise exercise = exerciseService.findExerciseById(exerciseId);
        if (exercise.getExerciseType() != ExerciseType.ESSAY) {
            throw new IllegalArgumentException("Exercise is not an essay type: " + exercise.getExerciseType());
        }

        // 2️⃣ Validate access
        validateAccess(exercise.getLesson(), request.getUserId());

        // 3️⃣ Next attempt number
        int attemptNumber = attemptRepository.findNextAttemptNumber(request.getUserId(), exerciseId);

        // 4️⃣ Compute word count
        int wordCount = countWords(request.getContent());

        // 5️⃣ Create WritingSubmission with status=PENDING
        WritingSubmission submission = WritingSubmission.builder()
                .userId(request.getUserId())
                .exercise(exercise)
                .attemptNumber(attemptNumber)
                .content(request.getContent())
                .wordCount(wordCount)
                .status(SubmissionStatus.PENDING)
                .build();

        WritingSubmission saved = writingRepository.save(submission);

        log.info("WritingSubmission created: submissionId={}, wordCount={}, attemptNumber={}",
                saved.getId(), wordCount, attemptNumber);

        // 6️⃣ Create ExerciseAttempt — userAnswer trỏ sang submissionId
        createLinkedAttempt(request.getUserId(), exercise, attemptNumber, saved.getId(), request.getTimeTakenSeconds());

        // 7️⃣ Publish event → AI Service
        publishWritingEvent(saved, exercise);

        return WritingSubmissionResponse.from(saved);
    }

    // ============================================
    // READ
    // ============================================

    /**
     * Get a speaking submission by ID — used for polling grading status.
     * Used in: GET /exercises/{id}/speaking/{submissionId}
     */
    @Transactional(readOnly = true)
    public SpeakingSubmissionResponse getSpeakingSubmission(UUID submissionId) {
        SpeakingSubmission submission = speakingRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException("Speaking submission not found: " + submissionId));
        return  SpeakingSubmissionResponse.from(submission);
    }

    /**
     * Get a writing submission by ID — used for polling grading status.
     * Used in: GET /exercises/{id}/writing/{submissionId}
     */
    @Transactional(readOnly = true)
    public WritingSubmissionResponse getWritingSubmission(UUID submissionId) {
        WritingSubmission submission = writingRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(
                        "Writing submission not found: " + submissionId));
        return WritingSubmissionResponse.from(submission);
    }

    // ============================================
    // HANDLE AI GRADING RESULT (Kafka consumer)
    // ============================================

    /**
     * Handle SubmissionGradedEvent from AI Service.
     * Routes to speaking or writing handler based on submissionType.
     *
     * Called by: SubmissionGradedEventListener (Kafka consumer — bước 6)
     */
    @Transactional
    public void handleGradingResult(SubmissionGradedEvent event) {

        log.info("Handling grading result: submissionId={}, type={}, success={}",
                event.getSubmissionId(), event.getSubmissionType(), event.getSuccess());

        if (!event.isValid()) {
            log.error("Invalid SubmissionGradedEvent: eventId={}", event.getEventId());
            return;
        }

        if (event.isSpeaking()) {
            handleSpeakingGraded(event);
        } else if (event.isWriting()) {
            handleWritingGraded(event);
        } else {
            log.error("Unknown submissionType: {}", event.getSubmissionType());
        }
    }

    // ============================================
    // INTERNAL — GRADING HANDLERS
    // ============================================

    private void handleSpeakingGraded(SubmissionGradedEvent event) {
        SpeakingSubmission submission = speakingRepository.findById(event.getSubmissionId())
                .orElseThrow(() -> new SubmissionNotFoundException(
                        "Speaking submission not found: " + event.getSubmissionId()));

        if (Boolean.TRUE.equals(event.getSuccess())) {
            submission.markGraded(
                    event.getAiScore(),
                    safeGet(event.getComponentScores(), "fluency"),
                    safeGet(event.getComponentScores(), "pronunciation"),
                    event.getFeedbackJson()
            );
            // Sync score to ExerciseAttempt → triggers LessonProgress update
            exerciseService.syncAiScore(
                    submission.getExercise().getId(),
                    submission.getUserId(),
                    submission.getAttemptNumber(),
                    event.getAiScore()
            );
        } else {
            submission.markFailed();
            log.warn("Speaking grading failed: submissionId={}, reason={}",
                    submission.getId(), event.getFailureReason());
        }

        speakingRepository.save(submission);
    }

    private void handleWritingGraded(SubmissionGradedEvent event) {
        WritingSubmission submission = writingRepository.findById(event.getSubmissionId())
                .orElseThrow(() -> new SubmissionNotFoundException(
                        "Writing submission not found: " + event.getSubmissionId()));

        if (Boolean.TRUE.equals(event.getSuccess())) {
            submission.markGraded(
                    event.getAiScore(),
                    safeGet(event.getComponentScores(), "grammar"),
                    safeGet(event.getComponentScores(), "vocabulary"),
                    safeGet(event.getComponentScores(), "coherence"),
                    safeGet(event.getComponentScores(), "taskAchievement"),
                    event.getFeedbackJson()
            );
            // Sync score to ExerciseAttempt → triggers LessonProgress update
            exerciseService.syncAiScore(
                    submission.getExercise().getId(),
                    submission.getUserId(),
                    submission.getAttemptNumber(),
                    event.getAiScore()
            );
        } else {
            submission.markFailed();
            log.warn("Writing grading failed: submissionId={}, reason={}",
                    submission.getId(), event.getFailureReason());
        }

        writingRepository.save(submission);
    }

    // ============================================
    // INTERNAL HELPERS
    // ============================================

    /**
     * Validate access: preview lesson = anyone, else need ACTIVE enrollment.
     */
    private void validateAccess(Lesson lesson, UUID userId) {
        if (lesson.getIsPreview()) return;

        UUID courseId = lesson.getSection().getCourse().getId();
        boolean isEnrolled = enrollmentRepository
                .findByUserIdAndCourseId(userId, courseId)
                .map(Enrollment::isActive)
                .orElse(false);

        if (!isEnrolled) {
            throw new NotEnrolledException(
                    "User must be enrolled to submit: courseId=" + courseId);
        }
    }

    /**
     * Create an ExerciseAttempt linked to a submission.
     * userAnswer = {"submission_id": "<submissionId>"}
     */
    private void createLinkedAttempt(UUID userId, Exercise exercise,
                                     int attemptNumber, UUID submissionId,
                                     Integer timeTakenSeconds) {
        ExerciseAttempt attempt = ExerciseAttempt.builder()
                .userId(userId)
                .exercise(exercise)
                .attemptNumber(attemptNumber)
                .userAnswer(Map.of("submission_id", submissionId.toString()))
                .timeTakenSeconds(timeTakenSeconds)
                // isCorrect + score = null → filled in after AI grading via syncAiScore()
                .build();

        attemptRepository.save(attempt);
    }

    /**
     * Count words in essay content.
     */
    private int countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return content.trim().split("\\s+").length;
    }

    private BigDecimal safeGet(
            Map<String, BigDecimal> map, String key) {
        return map != null ? map.getOrDefault(key, BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    // ============================================
    // KAFKA PUBLISHERS
    // ============================================

    private void publishSpeakingEvent(SpeakingSubmission submission, Exercise exercise) {
        try{
            SpeakingSubmissionCreatedEvent event = SpeakingSubmissionCreatedEvent.builder()
                    .submissionId(submission.getId())
                    .userId(submission.getUserId())
                    .exerciseId(exercise.getId())
                    .attemptNumber(submission.getAttemptNumber())
                    .audioUrl(submission.getAudioUrl())
                    .durationSeconds(submission.getDurationSeconds())
                    .submittedAt(submission.getSubmittedAt())
                    .build();

            kafkaTemplate.send(KafkaTopics.COURSE_SPEAKING_SUBMISSION_CREATED,
                    submission.getId().toString(), event);

            log.info("SpeakingSubmissionCreatedEvent published: submissionId={}", submission.getId());
        } catch (Exception e) {
            log.error("Failed to publish SpeakingSubmissionCreatedEvent: submissionId={}, error={}",
                    submission.getId(), e.getMessage());
        }
    }

    private void publishWritingEvent(WritingSubmission submission, Exercise exercise) {
        try {
            WritingSubmissionCreatedEvent event = WritingSubmissionCreatedEvent.builder()
                    .submissionId(submission.getId())
                    .userId(submission.getUserId())
                    .exerciseId(exercise.getId())
                    .attemptNumber(submission.getAttemptNumber())
                    .content(submission.getContent())
                    .wordCount(submission.getWordCount())
                    .submittedAt(submission.getSubmittedAt())
                    .build();

            kafkaTemplate.send(KafkaTopics.COURSE_WRITING_SUBMISSION_CREATED,
                    submission.getId().toString(), event);

            log.info("WritingSubmissionCreatedEvent published: submissionId={}", submission.getId());
        } catch (Exception e) {
            log.error("Failed to publish WritingSubmissionCreatedEvent: submissionId={}, error={}",
                    submission.getId(), e.getMessage());
        }
    }
}
