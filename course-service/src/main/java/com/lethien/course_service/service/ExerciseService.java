package com.lethien.course_service.service;

import com.lethien.course_service.dto.ExerciseAttemptRequest;
import com.lethien.course_service.dto.ExerciseAttemptResponse;
import com.lethien.course_service.entity.Enrollment;
import com.lethien.course_service.entity.Exercise;
import com.lethien.course_service.entity.ExerciseAttempt;
import com.lethien.course_service.entity.Lesson;
import com.lethien.course_service.exception.ExerciseNotFoundException;
import com.lethien.course_service.exception.NotEnrolledException;
import com.lethien.course_service.repository.EnrollmentRepository;
import com.lethien.course_service.repository.ExerciseAttemptRepository;
import com.lethien.course_service.repository.ExerciseRepository;
import com.lethien.course_service.repository.LessonProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExerciseService — handles exercise attempt submission and auto-grading.
 *
 * Grading rules:
 *   - Auto-grade: MCQ, FILL_IN_BLANK, TRUE_FALSE, MATCHING, ORDERING, SHORT_ANSWER, DICTATION
 *   - AI-grade  : ESSAY, SPEAKING_RECORD, SPEAKING_REPEAT → delegate to SubmissionService
 *
 * After grading:
 *   - LessonProgress.score = điểm cao nhất across tất cả attempts của lesson đó
 *   - markCompleted = true nếu score >= 60% tổng điểm của lesson
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseService {

    private static final int PASS_THRESHOLD_PERCENT = 60;

    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    // ============================================
    // SUBMIT ATTEMPT (auto-grade types)
    // ============================================

    /**
     * Submit an attempt for auto-graded exercise types.
     * ESSAY / SPEAKING_* must go through SubmissionService instead.
     *
     * Flow:
     *   1. Validate access (enrolled or preview lesson)
     *   2. Resolve next attempt number (no limit)
     *   3. Auto-grade answer
     *   4. Save ExerciseAttempt
     *   5. Update LessonProgress (best score across attempts)
     *   6. markCompleted if score >= 60%
     *
     * @param exerciseId target exercise
     * @param request    user answer payload
     */
    @Transactional
    public ExerciseAttemptResponse submitAttempt(UUID exerciseId, ExerciseAttemptRequest request) {

        log.info("Submitting attempt: exerciseId={}, userId={}", exerciseId, request.getUserId());

        // 1️⃣ Load exercise
        Exercise exercise = findExerciseById(exerciseId);

        // 2️⃣ AI-graded types must go through SubmissionService
        if (exercise.requiresAiGrading()) {
            throw new IllegalArgumentException(
                    "Exercise type " + exercise.getExerciseType() +
                            " must be submitted via /speaking or /writing endpoint"
            );
        }

        // 3️⃣ Validate access
        Lesson lesson = exercise.getLesson();
        validateAccess(lesson, request.getUserId());

        // 4️⃣ Next attempt number — no limit
        int attemptNumber = attemptRepository.findNextAttemptNumber(request.getUserId(), exerciseId);

        // 5️⃣ Auto-grade
        GradeResult result = autoGrade(exercise, request.getUserAnswer());

        // 6️⃣ Save attempt
        ExerciseAttempt attempt = ExerciseAttempt.builder()
                .userId(request.getUserId())
                .exercise(exercise)
                .attemptNumber(attemptNumber)
                .userAnswer(request.getUserAnswer())
                .isCorrect(result.isCorrect())
                .score(result.score())
                .timeTakenSeconds(request.getTimeTakenSeconds())
                .build();

        ExerciseAttempt saved = attemptRepository.save(attempt);

        log.info("Attempt saved: attemptId={}, isCorrect={}, score={}",
                saved.getId(), saved.getIsCorrect(), saved.getScore());

        // 7️⃣ Update lesson progress with best score
        updateLessonProgressAfterAttempt(lesson, request.getUserId(), exercise);

        return ExerciseAttemptResponse.from(saved);
    }

    /**
     * Called by SubmissionService after AI grading is complete.
     * Syncs AI score into ExerciseAttempt and updates LessonProgress.
     *
     * @param exerciseId   target exercise
     * @param userId       user who submitted
     * @param attemptNumber attempt number to update
     * @param aiScore      score from AI (0–100)
     */
    @Transactional
    public void syncAiScore(UUID exerciseId, UUID userId, Integer attemptNumber, BigDecimal aiScore) {

        log.info("Syncing AI score: exerciseId={}, userId={}, attempt={}, score={}",
                exerciseId, userId, attemptNumber, aiScore);

        ExerciseAttempt attempt = attemptRepository.findByUserIdAndExerciseIdAndAttemptNumber(userId, exerciseId, attemptNumber)
                .orElseThrow(() -> new ExerciseNotFoundException("Attempt not found: exerciseId=" + exerciseId + ", attempt=" + attemptNumber));

        // Convert AI score (0–100) to points based on exercise.points
        Exercise exercise = attempt.getExercise();
        BigDecimal scoreInPoints = aiScore
                .multiply(BigDecimal.valueOf(exercise.getPoints()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        attempt.setScore(scoreInPoints);
        attempt.setIsCorrect(aiScore.compareTo(BigDecimal.valueOf(PASS_THRESHOLD_PERCENT)) >= 0);

        attemptRepository.save(attempt);

        // Update lesson progress with new best score
        updateLessonProgressAfterAttempt(exercise.getLesson(), userId, exercise);

        log.info("AI score synced: attemptId={}, score={}", attempt.getId(), scoreInPoints);
    }


    // ============================================
    // READ
    // ============================================

    /**
     * Get all attempts by user for a specific exercise.
     * Used in: GET /exercises/{id}/attempts/me
     */
    @Transactional(readOnly = true)
    public List<ExerciseAttemptResponse> getMyAttempts(UUID exerciseId, UUID userId) {

        log.info("Getting attempts: exerciseId={}, userId={}", exerciseId, userId);

        return  attemptRepository
                .findAllByUserIdAndExerciseIdOrderByAttemptNumberDesc(userId, exerciseId)
                .stream()
                .map(ExerciseAttemptResponse::from)
                .toList();
    }

    // ============================================
    // INTERNAL — AUTO-GRADE
    // ============================================

    /**
     * Auto-grade user answer against exercise.correctAnswer.
     *
     * correctAnswer JSONB convention:
     *   MCQ / TRUE_FALSE / SHORT_ANSWER / DICTATION : {"answer": "A"}
     *   FILL_IN_BLANK                               : {"answers": ["word1", "word2"]}
     *   MATCHING  / ORDERING                        : {"order": [2,0,1]} | {"pairs": [[0,2]]}
     */
    private GradeResult autoGrade(Exercise exercise, Map<String, Object> userAnswer) {
        Map<String, Object> correct = exercise.getCorrectAnswer();
        boolean isCorrect;

        isCorrect = switch (exercise.getExerciseType()) {
            case MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, DICTATION ->
                    normalizeString(userAnswer.get("answer"))
                            .equalsIgnoreCase(normalizeString(correct.get("answer")));

            case FILL_IN_THE_BLANK -> {
                List<?> userAnswers = (List<?>) userAnswer.get("answers");
                List<?> correctAnswers = (List<?>) correct.get("answers");
                yield userAnswers != null
                        && correctAnswers != null
                        && userAnswers.size() == correctAnswers.size()
                        && userAnswers.stream()
                        .map(Object::toString)
                        .map(String::toLowerCase)
                        .toList()
                        .equals(correctAnswers.stream()
                                .map(Object::toString)
                                .map(String::toLowerCase)
                                .toList());
            }

            case MATCHING, ORDERING ->
                userAnswer.toString().equals(correct.toString());

            // AI-graded — should not reach here
            default -> false;
        };

        BigDecimal score = isCorrect
                ? BigDecimal.valueOf(exercise.getPoints())
                : BigDecimal.ZERO;

        return new GradeResult(isCorrect, score);
    }

    // ============================================
    // INTERNAL — LESSON PROGRESS UPDATE
    // ============================================

    /**
     * Recalculate LessonProgress.score after each attempt using best score strategy.
     *
     * Formula:
     *   lessonScore = SUM(best_score per exercise) / SUM(total points) * 100
     *
     * markCompleted = true if lessonScore >= 60
     */
    private void updateLessonProgressAfterAttempt(Lesson lesson, UUID userId, Exercise currentExercise) {
        // Find enrollment for this user + course
        UUID courseId = lesson.getSection().getCourse().getId();
        enrollmentRepository.findByUserIdAndCourseId(userId, courseId).ifPresent(enrollment -> {

            lessonProgressRepository
                    .findByEnrollmentIdAndLessonId(enrollment.getId(), lesson.getId())
                    .ifPresent(progress -> {

                        // Get all exercises in this lesson
                        List<Exercise> allExercises =
                                exerciseRepository.findAllByLessonIdOrderByOrderIndexAsc(lesson.getId());

                        // Calculate best score per exercise across all attempts
                        BigDecimal totalEarned = BigDecimal.ZERO;
                        BigDecimal totalPoints = BigDecimal.ZERO;

                        for (Exercise ex : allExercises) {
                            totalPoints = totalPoints.add(BigDecimal.valueOf(ex.getPoints()));

                            // Best score = MAX score across all attempts for this exercise
                            BigDecimal bestScore = attemptRepository
                                    .findAllByUserIdAndExerciseIdOrderByAttemptNumberDesc(userId, ex.getId())
                                    .stream()
                                    .map(ExerciseAttempt::getScore)
                                    .filter(s -> s != null)
                                    .max(BigDecimal::compareTo)
                                    .orElse(BigDecimal.ZERO);

                            totalEarned = totalEarned.add(bestScore);
                        }

                        // lessonScore = earned / total * 100
                        BigDecimal lessonScore = totalPoints.compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : totalEarned.multiply(BigDecimal.valueOf(100)).divide(totalPoints, 2, RoundingMode.HALF_UP);

                        progress.setScore(lessonScore);

                        // markCompleted if >= 60%
                        if (lessonScore.compareTo(BigDecimal.valueOf(PASS_THRESHOLD_PERCENT)) >= 0) {
                            progress.markCompleted(lessonScore);
                            log.info("Lesson passed: lessonId={}, userId={}, score={}",
                                    lesson.getId(), userId, lessonScore);
                        }

                        lessonProgressRepository.save(progress);
                    });
        });
    }

    // ============================================
    // INTERNAL — ACCESS VALIDATION
    // ============================================

    /**
     * Validate that user can access this lesson's exercises.
     *
     * Rules:
     *   lesson.isPreview = true  → anyone can attempt
     *   lesson.isPreview = false → must have ACTIVE enrollment
     */
    private void validateAccess(Lesson lesson, UUID userId) {
        if (lesson.getIsPreview()) {
            return; // public preview — no enrollment required
        }

        UUID courseId = lesson.getSection().getCourse().getId();
        boolean isEnrolled = enrollmentRepository
                .findByUserIdAndCourseId(userId, courseId)
                .map(Enrollment::isActive)
                .orElse(false);

        if (!isEnrolled) {
            throw new NotEnrolledException(
                    "User must be enrolled to attempt this exercise: courseId=" + courseId);
        }
    }

    // ============================================
    // INTERNAL HELPERS
    // ============================================

    public Exercise findExerciseById(UUID exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseNotFoundException("Exercise not found: " + exerciseId));
    }

    private String normalizeString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    /**
     * Immutable result of auto-grading.
     */
    private record GradeResult(boolean isCorrect, BigDecimal score) {}

}
