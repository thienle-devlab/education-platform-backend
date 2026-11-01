package com.lethien.course_service.service;

import com.lethien.course_service.dto.*;
import com.lethien.course_service.entity.*;
import com.lethien.course_service.exception.ContentModificationException;
import com.lethien.course_service.exception.CourseAccessDeniedException;
import com.lethien.course_service.exception.LessonNotFoundException;
import com.lethien.course_service.repository.EnrollmentRepository;
import com.lethien.course_service.repository.LessonRepository;
import com.lethien.course_service.repository.SectionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final LessonRepository lessonRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final SectionService sectionService;
    private final EntityManager entityManager;

    // ============================================
    // CREATE
    // ============================================

    /**
     * Tạo lesson mới — append vào cuối section (orderIndex = max + 1).
     * Nếu request có skillContent → tạo cùng lúc (luồng chính).
     * Trigger trigger_lessons_count tự cập nhật courses.total_lessons.
     */
    @Transactional
    public LessonResponse createLesson(UUID sectionId, UUID instructorId, CreateLessonRequest request) {
        log.info("Creating lesson: sectionId={}, instructorId={}", sectionId, instructorId);

        Section section = sectionService.findSectionById(sectionId);
        verifyOwnership(section.getCourse(), instructorId);

        // orderIndex = max +
        int nextOrder = lessonRepository
                .findMaxOrderIndexBySectionId(sectionId)
                .map(max -> max + 1)
                .orElse(0);

        Lesson lesson = Lesson.builder()
                .section(section)
                .title(request.getTitle())
                .description(request.getDescription())
                .lessonType(request.getLessonType())
                .orderIndex(nextOrder)
                .durationSeconds(request.getDurationSeconds())
                .contentUrl(request.getContentUrl())
                .isFree(request.getIsFree() != null ? request.getIsFree() : false)
                .build();

        Lesson saved = lessonRepository.save(lesson);

        // Tạo SkillContent cùng lúc nếu có
        if (request.getSkillContent() != null) {
            createSkillContent(saved, request.getSkillContent());
            // Refresh để load skillContent vừa tạo
            entityManager.refresh(saved);
        }

        log.info("Lesson created: lessonId={}, orderIndex={}", saved.getId(), nextOrder);
        return LessonResponse.from(saved);
    }

    // ============================================
    // READ
    // ============================================

    /**
     * Lấy danh sách lessons trong section — sorted by orderIndex.
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsBySection(UUID sectionId) {
        log.info("Getting lessons: sectionId={}", sectionId);

        sectionService.findSectionById(sectionId);

        return lessonRepository
                .findAllBySectionIdOrderByOrderIndexAsc(sectionId)
                .stream()
                .map(LessonResponse::from)
                .toList();
    }

    /**
     * Lấy toàn bộ curriculum (sections + lessons) của course.
     * Used in: GET /courses/{id}/curriculum
     */
    @Transactional(readOnly = true)
    public CurriculumResponse getCurriculum(UUID courseId) {
        log.info("Getting curriculum: courseId={}", courseId);

        Course course = courseService.findCourseById(courseId);

        List<Section> sections = sectionRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);

        List<CurriculumResponse.SectionWithLessons> sectionWithLessons = sections.stream()
                .map(CurriculumResponse.SectionWithLessons::from)
                .toList();

        int totalLessons = sectionWithLessons.stream()
                .mapToInt(CurriculumResponse.SectionWithLessons::getTotalLessons)
                .sum();

        return CurriculumResponse.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .totalSections(sections.size())
                .totalLessons(totalLessons)
                .sections(sectionWithLessons)
                .build();
    }

    /**
     * Lấy detail 1 lesson theo ID.
     */
    @Transactional(readOnly = true)
    public LessonResponse getLessonById(UUID lessonId) {
        return LessonResponse.from(findLessonById(lessonId));
    }

    // ============================================
    // UPDATE
    // ============================================

    /**
     * Sửa lesson metadata — partial update.
     * lessonType KHÔNG được sửa sau khi tạo.
     */
    @Transactional
    public LessonResponse updateLesson(UUID lessonId, UUID instructorId, UpdateLessonRequest request) {
        log.info("Updating lesson: lessonId={}", lessonId);

        Lesson lesson = findLessonById(lessonId);
        verifyOwnership(lesson.getSection().getCourse(), instructorId);

        if (request.getTitle() != null)           lesson.setTitle(request.getTitle());
        if (request.getDescription() != null)     lesson.setDescription(request.getDescription());
        if (request.getDurationSeconds() != null) lesson.setDurationSeconds(request.getDurationSeconds());
        if (request.getContentUrl() != null)      lesson.setContentUrl(request.getContentUrl());
        if (request.getIsPreview() != null)       lesson.setIsPreview(request.getIsPreview());
        if (request.getIsFree() != null)          lesson.setIsFree(request.getIsFree());

        Lesson updated = lessonRepository.save(lesson);
        log.info("Lesson updated: lessonId={}", lessonId);

        return LessonResponse.from(updated);
    }

    /**
     * Tạo hoặc cập nhật SkillContent của lesson.
     * Dùng khi muốn thêm SkillContent sau khi lesson đã tạo.
     */
    @Transactional
    public LessonResponse upsertSkillContent(UUID lessonId, UUID instructorId, SkillContentRequest request) {
        log.info("Upserting skill content: lessonId={}", lessonId);

        Lesson lesson = findLessonById(lessonId);
        verifyOwnership(lesson.getSection().getCourse(), instructorId);

        if (lesson.getSkillContent() != null) {
            // Update existing
            SkillContent sc = lesson.getSkillContent();
            if (request.getMediaUrl() != null)     sc.setMediaUrl(request.getMediaUrl());
            if (request.getThumbnailUrl() != null) sc.setThumbnailUrl(request.getThumbnailUrl());
            if (request.getTranscript() != null)   sc.setTranscript(request.getTranscript());
            if (request.getContentData() != null)  sc.setContentData(request.getContentData());
        } else {
            // Create new
            createSkillContent(lesson, request);
            entityManager.refresh(lesson);
        }

        Lesson updated = lessonRepository.save(lesson);
        log.info("SkillContent upserted: lessonId={}", lessonId);

        return LessonResponse.from(updated);
    }

    // ============================================
    // REORDER
    // ============================================

    /**
     * Đổi thứ tự lessons trong section.
     * lessonIds phải chứa đúng tất cả lessonId của section.
     */
    @Transactional
    public List<LessonResponse> reorderLessons(UUID sectionId, UUID instructorId, ReorderLessonsRequest request) {
        log.info("Reordering lessons: sectionId={}", sectionId);

        Section section = sectionService.findSectionById(sectionId);
        verifyOwnership(section.getCourse(), instructorId);

        List<Lesson> lessons = lessonRepository.findAllBySectionIdOrderByOrderIndexAsc(sectionId);

        if (lessons.size() != request.getLessonIds().size()) {
            throw new IllegalArgumentException(
                    "lessonIds count mismatch. Expected " + lessons.size()
                            + ", got " + request.getLessonIds().size());
        }

        List<UUID> existingIds = lessons.stream().map(Lesson::getId).toList();

        for (UUID id : request.getLessonIds()) {
            if (!existingIds.contains(id)) {
                throw new IllegalArgumentException(
                        "Lesson " + id + " does not belong to section " + sectionId);
            }
        }

        for (int i = 0; i < request.getLessonIds().size(); i ++) {
            UUID lessonId = request.getLessonIds().get(i);
            Lesson lesson = lessons.stream()
                    .filter(l -> l.getId().equals(lessonId))
                    .findFirst().orElseThrow();
            lesson.setOrderIndex(i);
            lessonRepository.save(lesson);
        }

        log.info("Lessons reordered: sectionId={}", sectionId);

        return lessonRepository
                .findAllBySectionIdOrderByOrderIndexAsc(sectionId)
                .stream()
                .map(LessonResponse::from)
                .toList();
    }

    // ============================================
    // DELETE
    // ============================================

    /**
     * Xóa lesson — cascade xóa exercises, submissions, lesson_progress.
     * Trigger trigger_lessons_count tự cập nhật total_lessons.
     *
     * An toàn dữ liệu:
     *   - Course DRAFT → xóa tự do
     *   - Course PUBLISHED/ARCHIVED có enrollments → CHẶN
     */
    @Transactional
    public void deleteLesson(UUID lessonId, UUID instructorId) {
        log.info("Deleting lesson: lessonId={}", lessonId);

        Lesson lesson = findLessonById(lessonId);
        Course course = lesson.getSection().getCourse();
        verifyOwnership(course, instructorId);

        if (course.getStatus() != CourseStatus.DRAFT) {
            long enrollmentCount = enrollmentRepository.countByCourseId(course.getId());
            if (enrollmentCount > 0) {
                throw new ContentModificationException(
                        "Cannot delete lesson: course has " + enrollmentCount +
                                " active enrollment(s). Archive the course first.");
            }
        }

        lessonRepository.delete(lesson);
        log.info("Lesson deleted: lessonId={}", lessonId);
    }

    // ============================================
    // INTERNAL HELPERS
    // ============================================
    public Lesson findLessonById(UUID lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(
                        "Lesson not found: " + lessonId));
    }

    private void createSkillContent(Lesson lesson, SkillContentRequest request) {
        SkillContent sc = SkillContent.builder()
                .lesson(lesson)
                .skillType(request.getSkillType())
                .mediaUrl(request.getMediaUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .transcript(request.getTranscript())
                .contentData(request.getContentData() != null
                        ? request.getContentData() : Map.of())
                .build();
        lesson.setSkillContent(sc);
    }

    private void verifyOwnership(Course course, UUID instructorId) {
        if (!course.getInstructorId().equals(instructorId)) {
            throw new CourseAccessDeniedException(
                    "Access denied: you are not the owner of this course");
        }
    }
}
