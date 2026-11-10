package com.lethien.course_service.service;

import com.lethien.course_service.dto.CreateExerciseRequest;
import com.lethien.course_service.dto.ExerciseResponse;
import com.lethien.course_service.dto.ReorderExercisesRequest;
import com.lethien.course_service.dto.UpdateExerciseRequest;
import com.lethien.course_service.entity.*;
import com.lethien.course_service.exception.ContentModificationException;
import com.lethien.course_service.exception.CourseAccessDeniedException;
import com.lethien.course_service.exception.ExerciseNotFoundException;
import com.lethien.course_service.exception.NotEnrolledException;
import com.lethien.course_service.repository.EnrollmentRepository;
import com.lethien.course_service.repository.ExerciseAttemptRepository;
import com.lethien.course_service.repository.ExerciseRepository;
import com.lethien.course_service.validation.ExerciseContentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ExerciseManagementService — instructor CRUD cho exercises.
 *
 * Tách riêng khỏi ExerciseService (chấm bài) để phần quản lý nội dung
 * và phần học viên làm bài không lẫn quyền với nhau.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseManagementService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonService             lessonService;

    // ============================================
    // CREATE
    // ============================================

    @Transactional
    public ExerciseResponse createExercise(UUID lessonId, UUID instructorId, CreateExerciseRequest request) {
        log.info("Creating exercise: lessonId={}, type={}", lessonId, request.getExerciseType());

        Lesson lesson = lessonService.findLessonById(lessonId);
        verifyOwnership(lesson.getSection().getCourse(), instructorId);

        if (lesson.getLessonType() != LessonType.EXERCISE) {
            throw new IllegalArgumentException(
                    "Exercises can only be added to lessons of type EXERCISE (this lesson is "
                            + lesson.getLessonType() + ")");
        }

        // ESSAY / SPEAKING_* không cần đáp án → lưu {} (cột correct_answer là NOT NULL)
        Map<String, Object> correct =
                request.getCorrectAnswer() != null ? request.getCorrectAnswer() : Map.of();
        ExerciseContentValidator.validate(request.getExerciseType(), request.getOptions(), correct);

        int nextOrder = exerciseRepository.findMaxOrderIndexByLessonId(lessonId)
                .map(max -> max + 1)
                .orElse(0);

        Exercise exercise = Exercise.builder()
                .lesson(lesson)
                .exerciseType(request.getExerciseType())
                .question(request.getQuestion())
                .questionAudioUrl(request.getQuestionAudioUrl())
                .options(request.getOptions())
                .correctAnswer(correct)
                .explanation(request.getExplanation())
                .points(request.getPoints() != null ? request.getPoints() : 1)
                .orderIndex(nextOrder)
                .build();

        Exercise saved  = exerciseRepository.save(exercise);
        log.info("Exercise created: exerciseId={}, orderIndex={}", saved.getId(), nextOrder);

        return ExerciseResponse.forOwner(saved);
    }

    // ============================================
    // READ
    // ============================================

    /**
     * Owner của course → thấy đầy đủ (kèm correctAnswer).
     * Học viên → chỉ thấy khi lesson là preview của course PUBLISHED, hoặc đang enrolled
     *            (ACTIVE / COMPLETED), và KHÔNG thấy correctAnswer.
     */
    @Transactional(readOnly = true)
    public List<ExerciseResponse> getExercises(UUID lessonId, UUID userId) {
        Lesson lesson = lessonService.findLessonById(lessonId);
        Course course = lesson.getSection().getCourse();

        boolean owner = course.getInstructorId().equals(userId);
        if (!owner) {
            verifyLearnerAccess(lesson, course, userId);
        }

        return exerciseRepository.findAllByLessonIdOrderByOrderIndexAsc(lessonId)
                .stream()
                .map(e -> owner ? ExerciseResponse.forOwner(e) : ExerciseResponse.forLearner(e))
                .toList();
    }

    // ============================================
    // UPDATE
    // ============================================

    @Transactional
    public ExerciseResponse updateExercise(UUID exerciseId, UUID instructorId, UpdateExerciseRequest request) {
        log.info("Updating exercise: exerciseId={}", exerciseId);

        Exercise exercise = findExerciseById(exerciseId);
        verifyOwnership(exercise.getLesson().getSection().getCourse(), instructorId);

        boolean touchesGrading = request.getExerciseType() != null
                || request.getOptions() != null
                || request.getCorrectAnswer() != null
                || request.getPoints() != null;

        if (touchesGrading) {
            if (attemptRepository.existsByExerciseId(exerciseId)) {
                throw new ContentModificationException(
                        "Exercise already has attempts: exerciseType, options, correctAnswer "
                                + "and points can no longer be changed");
            }

            // Validate trên trạng thái SAU KHI gộp (field mới đè field cũ)
            ExerciseType type = request.getExerciseType() != null
                    ? request.getExerciseType() :  exercise.getExerciseType();
            Map<String, Object> options = request.getOptions() != null
                    ? request.getOptions() : exercise.getOptions();
            Map<String, Object> correct = request.getCorrectAnswer() != null
                    ? request.getCorrectAnswer() : exercise.getCorrectAnswer();
            ExerciseContentValidator.validate(type, options, correct);

            exercise.setExerciseType(type);
            exercise.setOptions(options);
            exercise.setCorrectAnswer(correct);
            if (request.getPoints() != null) exercise.setPoints(request.getPoints());
        }

        if (request.getQuestion() != null)         exercise.setQuestion(request.getQuestion());
        if (request.getQuestionAudioUrl() != null) exercise.setQuestionAudioUrl(request.getQuestionAudioUrl());
        if (request.getExplanation() != null)      exercise.setExplanation(request.getExplanation());

        Exercise updated = exerciseRepository.save(exercise);
        log.info("Exercise updated: exerciseId={}", exerciseId);

        return ExerciseResponse.forOwner(updated);
    }

    // ============================================
    // REORDER
    // ============================================

    /**
     * Bảng exercises KHÔNG có UNIQUE(lesson_id, order_index) nên gán thẳng 0..n-1 là an toàn
     * (khác sections và lessons — xem ReorderFix trong thư mục này).
     */
    @Transactional
    public List<ExerciseResponse> reorderExercises(UUID lessonId, UUID instructorId, ReorderExercisesRequest request) {
        log.info("Reordering exercises: lessonId={}", lessonId);

        Lesson lesson = lessonService.findLessonById(lessonId);
        verifyOwnership(lesson.getSection().getCourse(), instructorId);

        List<Exercise> exercises = exerciseRepository.findAllByLessonIdOrderByOrderIndexAsc(lessonId);
        List<UUID> ids = request.getExerciseIds();

        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("exerciseIds contains duplicates");
        }
        if (ids.size() != exercises.size()) {
            throw new IllegalArgumentException(
                    "exerciseIds count mismatch. Expected " + exercises.size() + ", got " + ids.size());
        }

        Map<UUID, Exercise> byId = exercises.stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));
        for (UUID id : ids) {
            if (!byId.containsKey(id)) {
                throw new IllegalArgumentException(
                        "Exercise " + id + " does not belong to lesson " + lessonId);
            }
        }
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).setOrderIndex(i);
        }
        exerciseRepository.saveAll(exercises);

        return exercises.stream()
                .sorted(Comparator.comparing(Exercise::getOrderIndex))
                .map(ExerciseResponse::forOwner)
                .toList();
    }

    // ============================================
    // DELETE
    // ============================================

    /**
     * Cùng rule với Section/Lesson:
     *   - Course DRAFT → xóa tự do
     *   - Course PUBLISHED/ARCHIVED có enrollments → CHẶN
     * Attempts / submissions của exercise bị DB cascade (ON DELETE CASCADE).
     */
    @Transactional
    public void deleteExercise(UUID exerciseId, UUID instructorId) {
        log.info("Deleting exercise: exerciseId={}", exerciseId);

        Exercise exercise = findExerciseById(exerciseId);
        Course course = exercise.getLesson().getSection().getCourse();
        verifyOwnership(course, instructorId);

        if (course.getStatus() != CourseStatus.DRAFT) {
            long enrollmentCount = enrollmentRepository.countByCourseId(course.getId());
            if (enrollmentCount > 0) {
                throw new ContentModificationException(
                        "Cannot delete exercise: course has " + enrollmentCount
                                + " enrollment(s). Archive the course first.");
            }
        }

        exerciseRepository.delete(exercise);
        log.info("Exercise deleted: exerciseId={}", exerciseId);
    }

    // ============================================
    // INTERNAL HELPERS
    // ============================================

    private Exercise findExerciseById(UUID exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseNotFoundException("Exercise not found: " + exerciseId));
    }

    private void verifyLearnerAccess(Lesson lesson, Course course, UUID userId) {
        boolean enrolled = enrollmentRepository
                .findByUserIdAndCourseId(userId, course.getId())
                .map(e -> e.getStatus() == EnrollmentStatus.ACTIVE
                        || e.getStatus() == EnrollmentStatus.COMPLETED)
                .orElse(false);
        if (enrolled) return;

        // Preview chỉ có hiệu lực khi course đã PUBLISHED — tránh lộ nội dung course DRAFT
        if (Boolean.TRUE.equals(lesson.getIsPreview()) && course.isPublished()) return;

        throw new NotEnrolledException("You must be enrolled in this course to view its exercises");
    }

    private void verifyOwnership(Course course, UUID instructorId) {
        if (!course.getInstructorId().equals(instructorId)) {
            throw new CourseAccessDeniedException(
                    "Access denied: you are not the owner of this course");
        }
    }
}
