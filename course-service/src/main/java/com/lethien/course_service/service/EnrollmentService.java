package com.lethien.course_service.service;

import com.lethien.common_lib.constant.KafkaTopics;
import com.lethien.common_lib.dto.PageResponse;
import com.lethien.common_lib.event.EnrollmentCreatedEvent;
import com.lethien.course_service.dto.EnrollmentResponse;
import com.lethien.course_service.dto.LessonProgressResponse;
import com.lethien.course_service.dto.UpdateLessonProgressRequest;
import com.lethien.course_service.entity.*;
import com.lethien.course_service.exception.AlreadyEnrolledException;
import com.lethien.course_service.exception.CourseNotFoundException;
import com.lethien.course_service.exception.EnrollmentNotFoundException;
import com.lethien.course_service.exception.NotEnrolledException;
import com.lethien.course_service.repository.CourseRepository;
import com.lethien.course_service.repository.EnrollmentRepository;
import com.lethien.course_service.repository.LessonProgressRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseRepository courseRepository;
    private final EntityManager entityManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CourseService courseService;

    // ============================================
    // ENROLL
    // ============================================

    /**
     * Enroll a user into a PUBLISHED course.
     *
     * Cases:
     *   1. No existing enrollment → create new
     *   2. ACTIVE/COMPLETED       → throw AlreadyEnrolledException
     *   3. CANCELLED/EXPIRED      → reactivate existing enrollment
     *
     * @param userId   soft ref → users.id in User Service
     * @param courseId target course
     */
    @Transactional
    public EnrollmentResponse enroll(UUID userId, UUID courseId) {
        log.info("Enrolling user: userId={}, courseId={}", userId, courseId);

        // 1️⃣ Course must be PUBLISHED
        Course course = courseService.findCourseById(courseId);
        if (!course.isPublished()) {
            throw new CourseNotFoundException("Course is not available for enrollment: " + courseId);
        }

        // 2️⃣ Check existing enrollment
        Optional<Enrollment> existing = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);

        if (existing.isPresent()) {
            Enrollment enrollment = existing.get();

            if (enrollment.isActive() || enrollment.isCompleted()) {
                throw new AlreadyEnrolledException("User is already enrolled in this course: courseId=" + courseId);
            }

            // 3️⃣ Reactivate CANCELLED / EXPIRED enrollment
            log.info("Reactivating enrollment: enrollmentId={}", enrollment.getId());
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setCompletedAt(null);
            enrollment.setExpiredAt(null);
            enrollment.recordAccess();

            Enrollment reactivated = enrollmentRepository.save(enrollment);

            log.info("Enrollment reactivated: enrollmentId={}", reactivated.getId());
            return EnrollmentResponse.from(reactivated);
        }

        // 4️⃣ Create new enrollment
        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);

        log.info("Enrollment created: enrollmentId={}, userId={}, courseId={}", saved.getId(), userId, courseId);

        // 5️⃣ Publish event → Notification Service
        publishEnrollmentCreatedEvent(saved, course);

        return EnrollmentResponse.from(saved);
    }

    // ============================================
    // READ
    // ============================================

    /**
     * Get a specific enrollment by ID.
     * Used in: GET /enrollments/{id}
     */
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(UUID enrollmentId) {
        log.info("Getting enrollment: enrollmentId={}", enrollmentId);

        Enrollment enrollment = findEnrollmentById(enrollmentId);

        return EnrollmentResponse.from(enrollment);
    }

    /**
     * Get all enrollments of a user with pagination.
     * Used in: GET /enrollments/me
     */
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> getMyEnrollments(UUID userId, Pageable pageable) {
        log.info("Getting enrollments: userId={}", userId);

        Page<Enrollment> page = enrollmentRepository.findAllByUserId(userId, pageable);
        return PageResponse.of(page.map(EnrollmentResponse::from));
    }

    /**
     * Get all enrollments of a user filtered by status.
     * Used in: GET /enrollments/me?status=ACTIVE
     */
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> getMyEnrollmentsByStatus(
            UUID userId, EnrollmentStatus status, Pageable pageable) {

        log.info("Getting enrollments by status: userId={}, status={}", userId, status);

        Page<Enrollment> page = enrollmentRepository.findAllByUserIdAndStatus(userId, status, pageable);
        return PageResponse.of(page.map(EnrollmentResponse::from));
    }

    /**
     * Get all lesson progress entries for an enrollment.
     * Used in: GET /enrollments/{id}/progress
     */
    @Transactional(readOnly = true)
    public List<LessonProgressResponse> getLessonProgress(UUID enrollmentId) {
        log.info("Getting lesson progress: enrollmentId={}", enrollmentId);

        // Verify enrollment exists
        findEnrollmentById(enrollmentId);

        return lessonProgressRepository.findAllByEnrollmentId(enrollmentId)
                .stream()
                .map(LessonProgressResponse::from)
                .toList();
    }

    // ============================================
    // UPDATE LESSON PROGRESS
    // ============================================

    /**
     * Update lesson progress for an enrolled user.
     *
     * Flow:
     *   NOT_STARTED → IN_PROGRESS (first access)
     *   IN_PROGRESS → COMPLETED   (when markCompleted=true)
     *
     * After save: entityManager.refresh(enrollment) để lấy progressPercent
     * mới nhất được tính bởi trigger trigger_progress_update.
     *
     * Nếu tất cả bài đã hoàn thành → tự động mark enrollment COMPLETED.
     *
     * @param enrollmentId target enrollment
     * @param lessonId     target lesson
     * @param request      update payload
     */
    @Transactional
    public LessonProgressResponse updateLessonProgress(
            UUID enrollmentId, UUID lessonId, UpdateLessonProgressRequest request
    ) {

        log.info("Updating lesson progress: enrollmentId={}, lessonId={}", enrollmentId, lessonId);

        // 1️⃣ Verify enrollment is ACTIVE
        Enrollment enrollment = findEnrollmentById(enrollmentId);
        if (!enrollment.isActive()) {
            throw new NotEnrolledException("Enrollment is not active: enrollmentId=" + enrollmentId);
        }

        // 2️⃣ Upsert LessonProgress
        LessonProgress progress = lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                .orElseGet(() -> {
                    // First time accessing this lesson → create new record
                    Lesson lesson = entityManager.getReference(Lesson.class, lessonId);

                    return LessonProgress.builder()
                            .enrollment(enrollment)
                            .lesson(lesson)
                            .status(ProgressStatus.NOT_STARTED)
                            .build();
                });

        // 3️⃣ Apply state transitions
        if (Boolean.TRUE.equals(request.getMarkCompleted())) {
            progress.markCompleted(null); // score set later by ExerciseService if needed
        } else {
            progress.markInProgress();
        }

        // 4️⃣ Accumulate time spent
        if (request.getTimeSpentSeconds() != null && request.getTimeSpentSeconds() > 0) {
            progress.addTimeSpent(request.getTimeSpentSeconds());
        }

        // 5️⃣ Record last access on enrollment
        enrollment.recordAccess();
        enrollmentRepository.save(enrollment);

        LessonProgress saved = lessonProgressRepository.save(progress);

        // 6️⃣ Refresh enrollment — trigger trigger_progress_update đã chạy,
        //    cần refresh để Hibernate thấy progressPercent mới nhất từ DB
        entityManager.refresh(enrollment);

        log.info("Lesson progress updated: enrollmentId={}, lessonId={}, status={}", enrollmentId, lessonId, saved.getStatus());

        // 7️⃣ Auto-complete enrollment if all lessons done
        checkAndCompleteEnrollment(enrollment);

        return LessonProgressResponse.from(saved);

    }

    // ============================================
    // CANCEL
    // ============================================

    /**
     * Cancel an ACTIVE enrollment.
     * Progress is preserved — user can re-enroll and continue later.
     */
    @Transactional
    public EnrollmentResponse cancelEnrollment(UUID enrollmentId, UUID userId) {

        log.info("Cancelling enrollment: enrollmentId={}, userId={}", enrollmentId, userId);

        Enrollment enrollment = findEnrollmentById(enrollmentId);

        // Ownership check
        if (!enrollment.getUserId().equals(userId)) {
            throw new NotEnrolledException("Access denied: enrollmentId=" + enrollmentId);
        }

        if (!enrollment.isActive()) {
            throw new NotEnrolledException("Only ACTIVE enrollments can be cancelled: enrollmentId=" + enrollmentId);
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        log.info("Enrollment cancelled: enrollmentId={}", enrollmentId);

        return EnrollmentResponse.from(saved);
    }

    // ============================================
    // INTERNAL HELPERS
    // ============================================

    /**
     * Find enrollment by ID or throw EnrollmentNotFoundException.
     */
    public Enrollment findEnrollmentById(UUID enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException("Enrollment not found: " + enrollmentId));
    }

    /**
     * Auto-complete enrollment when progressPercent = 100.
     * Triggered after each lesson progress update.
     */
    private void checkAndCompleteEnrollment(Enrollment enrollment) {
        // progressPercent đã được trigger cập nhật và entityManager.refresh() đã lấy về
        if (enrollment.getProgressPercent() != null
                && enrollment.getProgressPercent().intValue() == 100
                && enrollment.isActive()) {

            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(ZonedDateTime.now());
            enrollmentRepository.save(enrollment);

            log.info("Enrollment auto-completed: enrollmentId={}, userId={}, courseId={}",
                    enrollment.getId(), enrollment.getUserId(), enrollment.getCourse().getId());
        }
    }

    /**
     * Publish EnrollmentCreatedEvent to Kafka.
     */
    private void publishEnrollmentCreatedEvent(Enrollment enrollment, Course course) {

        try {
            EnrollmentCreatedEvent event = EnrollmentCreatedEvent.builder()
                    .enrollmentId(enrollment.getId())
                    .userId(enrollment.getUserId())
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .courseSkillType(course.getSkillType().name())
                    .enrolledAt(enrollment.getEnrolledAt())
                    .build();

            kafkaTemplate.send(KafkaTopics.COURSE_ENROLLMENT_CREATED, enrollment.getId().toString(), event);

            log.info("EnrollmentCreatedEvent published: enrollmentId={}", enrollment.getId());
        } catch (Exception e) {
            // Không fail enrollment nếu Kafka lỗi — event loss acceptable ở đây
            log.error("Failed to publish EnrollmentCreatedEvent: enrollmentId={}, error={}",
                    enrollment.getId(), e.getMessage());
        }
    }
}
