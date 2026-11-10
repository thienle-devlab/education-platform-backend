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
import com.lethien.course_service.service.CourseAccessService.ViewerAccess;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final LessonRepository lessonRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final SectionService sectionService;
    private final CourseAccessService courseAccessService;
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

            saved = lessonRepository.saveAndFlush(saved);
        }

        log.info("Lesson created: lessonId={}, orderIndex={}", saved.getId(), nextOrder);
        return LessonResponse.from(saved);
    }

    // ============================================
    // READ
    // ============================================

    /**
     * Lấy danh sách lessons trong section.
     * Nội dung của lesson bị khóa với người xem PUBLIC (trừ lesson preview).
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsBySection(UUID sectionId, UUID viewerUserId) {
        log.info("Getting lessons: sectionId={}", sectionId);

        Section section = sectionService.findSectionById(sectionId);
        ViewerAccess access = courseAccessService.resolveForReading(section.getCourse(), viewerUserId);

        return lessonRepository
                .findAllBySectionIdOrderByOrderIndexAsc(sectionId)
                .stream()
                .map(l -> LessonResponse.from(l, canSeeContent(access, l)))
                .toList();
    }

    /**
     * Lấy toàn bộ curriculum (sections + lessons) của course.
     * Used in: GET /courses/{id}/curriculum
     */
    @Transactional(readOnly = true)
    public CurriculumResponse getCurriculum(UUID courseId, UUID viewerUserId) {
        log.info("Getting curriculum: courseId={}", courseId);

        Course course = courseService.findCourseById(courseId);
        ViewerAccess access = courseAccessService.resolveForReading(course, viewerUserId);
        boolean fullAccess = access.canViewAllContent();

        List<Section> sections =
                sectionRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);

        List<CurriculumResponse.SectionWithLessons> sectionWithLessons = sections.stream()
                .map(s -> CurriculumResponse.SectionWithLessons.from(s, fullAccess))
                .toList();

        int totalLessons = sectionWithLessons.stream()
                .mapToInt(CurriculumResponse.SectionWithLessons::getTotalLessons)
                .sum();

        return CurriculumResponse.builder()
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .viewerAccess(access.name())
                .totalSections(sections.size())
                .totalLessons(totalLessons)
                .sections(sectionWithLessons)
                .build();
    }

    /**
     * Lấy detail 1 lesson theo ID.
     * Lesson bị khóa vẫn trả 200 (kèm contentLocked = true, không có contentUrl/skillContent).
     */
    @Transactional(readOnly = true)
    public LessonResponse getLessonById(UUID lessonId, UUID viewerUserId) {
        Lesson lesson = findLessonById(lessonId);
        ViewerAccess access = courseAccessService.resolveForReading(
                lesson.getSection().getCourse(), viewerUserId);

        return LessonResponse.from(lesson, canSeeContent(access, lesson));
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
        }

        Lesson updated = lessonRepository.saveAndFlush(lesson);
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

        if (new HashSet<>(request.getLessonIds()).size() != request.getLessonIds().size()) {
            throw new IllegalArgumentException("sectionIds contains duplicates");
        }

        List<UUID> existingIds = lessons.stream().map(Lesson::getId).toList();

        for (UUID id : request.getLessonIds()) {
            if (!existingIds.contains(id)) {
                throw new IllegalArgumentException(
                        "Lesson " + id + " does not belong to section " + sectionId);
            }
        }

        // Pha 1: dời tạm tất cả ra ngoài vùng 0..n-1 để không bị trùng khi đổi chỗ
        int offset = lessons.stream().mapToInt(Lesson::getOrderIndex).max().orElse(0)
                + lessons.size() + 1;
        for (Lesson l : lessons) {
            l.setOrderIndex(l.getOrderIndex() + offset);
        }
        lessonRepository.saveAllAndFlush(lessons);

// Pha 2: gán thứ tự cuối cùng 0..n-1
        Map<UUID, Lesson> byId = lessons.stream()
                .collect(Collectors.toMap(Lesson::getId, l -> l));
        for (int i = 0; i < request.getLessonIds().size(); i++) {
            byId.get(request.getLessonIds().get(i)).setOrderIndex(i);
        }
        lessonRepository.saveAllAndFlush(lessons);

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

    private boolean canSeeContent(ViewerAccess access, Lesson lesson) {
        return access.canViewAllContent() || Boolean.TRUE.equals(lesson.getIsPreview());
    }

    private void verifyOwnership(Course course, UUID instructorId) {
        if (!course.getInstructorId().equals(instructorId)) {
            throw new CourseAccessDeniedException(
                    "Access denied: you are not the owner of this course");
        }
    }
}
