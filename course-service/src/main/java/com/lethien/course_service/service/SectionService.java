package com.lethien.course_service.service;

import com.lethien.course_service.dto.CreateSectionRequest;
import com.lethien.course_service.dto.ReorderSectionsRequest;
import com.lethien.course_service.dto.SectionResponse;
import com.lethien.course_service.dto.UpdateSectionRequest;
import com.lethien.course_service.entity.Course;
import com.lethien.course_service.entity.CourseStatus;
import com.lethien.course_service.entity.Section;
import com.lethien.course_service.exception.ContentModificationException;
import com.lethien.course_service.exception.CourseAccessDeniedException;
import com.lethien.course_service.exception.SectionNotFoundException;
import com.lethien.course_service.repository.EnrollmentRepository;
import com.lethien.course_service.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionService {

    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;

    // ============================================
    // CREATE
    // ============================================

    /**
     * Tạo section mới — append vào cuối (orderIndex = max + 1).
     * Chỉ INSTRUCTOR owner mới được tạo.
     * Course có thể ở bất kỳ status nào (DRAFT, PUBLISHED, ARCHIVED).
     */
    @Transactional
    public SectionResponse createSection(UUID courseId, UUID instructorId, CreateSectionRequest request) {

        log.info("Creating section: courseId={}, instructorId={}", courseId, instructorId);

        Course course = courseService.findCourseById(courseId);
        verifyOwnership(course, instructorId);

        // orderIndex = max hiện tại + 1
        int nextOrder = sectionRepository
                .findMaxOrderIndexByCourseId(courseId)
                .map(max -> max + 1)
                .orElse(0);

        Section section = Section.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .orderIndex(nextOrder)
                .build();

        Section saved = sectionRepository.save(section);
        log.info("Section created: sectionId={}, orderIndex={}", saved.getId(), nextOrder);

        return SectionResponse.from(saved);
    }

    // ============================================
    // READ
    // ============================================

    /**
     * Lấy danh sách sections của course — sorted by orderIndex.
     * Public — không cần check ownership.
     */
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByCourse(UUID courseId) {

        log.info("Getting sections: courseId={}", courseId);

        courseService.findCourseById(courseId);

        return sectionRepository
                .findAllByCourseIdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(SectionResponse::from)
                .toList();
    }

    // ============================================
    // UPDATE
    // ============================================

    /**
     * Sửa title/description của section — partial update.
     */
    @Transactional
    public SectionResponse updateSection(UUID sectionId, UUID instructorId, UpdateSectionRequest request) {

        log.info("Updating section: sectionId={}", sectionId);

        Section section = findSectionById(sectionId);
        verifyOwnership(section.getCourse(), instructorId);

        if (request.getTitle() != null) section.setTitle(request.getTitle());

        if (request.getDescription() != null) section.setDescription(request.getDescription());

        Section updated = sectionRepository.save(section);
        log.info("Section updated: sectionId={}", sectionId);

        return SectionResponse.from(updated);
    }

    // ============================================
    // REORDER
    // ============================================

    /**
     * Đổi thứ tự sections trong course.
     *
     * sectionIds phải chứa đúng tất cả sectionId của course,
     * không được thiếu hoặc thừa.
     *
     * @param courseId    target course
     * @param instructorId phải là owner
     * @param request     danh sách sectionIds theo thứ tự mới
     */
    @Transactional
    public List<SectionResponse> reorderSections(UUID courseId, UUID instructorId, ReorderSectionsRequest request) {

        log.info("Reordering sections: courseId={}", courseId);

        Course course = courseService.findCourseById(courseId);
        verifyOwnership(course, instructorId);

        List<Section> sections =
                sectionRepository.findAllByCourseIdOrderByOrderIndexAsc(courseId);

        // Validate: số lượng phải khớp
        if (sections.size() != request.getSectionIds().size()) {
            throw new IllegalArgumentException(
                    "sectionIds count mismatch. Expected " + sections.size()
                            + ", got " + request.getSectionIds().size()
            );
        }

        // Validate: tất cả sectionId phải thuộc course này
        List<UUID> existingIds = sections.stream()
                .map(Section::getId).toList();
        for (UUID id : request.getSectionIds()) {
            if (!existingIds.contains(id)) {
                throw new IllegalArgumentException(
                        "Section " + id + " does not belong to course " + courseId);
            }
        }

        // Áp dụng thứ tự mới
        for (int i = 0; i < request.getSectionIds().size(); i ++) {
            UUID sectionId = request.getSectionIds().get(i);
            Section section = sections.stream()
                    .filter(s -> s.getId().equals(sectionId))
                    .findFirst()
                    .orElseThrow();
            section.setOrderIndex(i);
            sectionRepository.save(section);
        }

        log.info("Sections reordered: courseId={}", courseId);

        return sectionRepository
                .findAllByCourseIdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(SectionResponse::from)
                .toList();
    }

    // ============================================
    // DELETE
    // ============================================

    /**
     * Xóa section — cascade xóa tất cả lessons và exercises bên trong.
     *
     * An toàn dữ liệu:
     *   - Course DRAFT → xóa tự do
     *   - Course PUBLISHED/ARCHIVED có enrollments → CHẶN xóa
     *     vì sẽ ảnh hưởng LessonProgress của học viên đang học
     *
     * Nếu muốn xóa section của course đã published:
     *   → Archive course trước → xóa section → publish lại
     */
    @Transactional
    public void deleteSection(UUID sectionId, UUID instructorId) {

        log.info("Deleting section: sectionId={}", sectionId);

        Section section = findSectionById(sectionId);
        Course course = section.getCourse();
        verifyOwnership(course, instructorId);

        // An toàn dữ liệu: chặn nếu course đã có enrollments
        if (course.getStatus() != CourseStatus.DRAFT) {
            long enrollmentCount = enrollmentRepository.countByCourseId(course.getId());

            if (enrollmentCount > 0) {
                throw new ContentModificationException(
                        "Cannot delete section: course has " + enrollmentCount +
                                " active enrollment(s). Archive the course first.");
            }
        }

        sectionRepository.delete(section);
        log.info("Section deleted: sectionId={}", sectionId);
    }

    // ============================================
    // INTERNAL HELPERS
    // ============================================

    public Section findSectionById(UUID sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(
                        "Section not found: " + sectionId));
    }

    private void verifyOwnership(Course course, UUID instructorId) {
        if (!course.getInstructorId().equals(instructorId)) {
            throw new CourseAccessDeniedException(
                    "Access denied: you are not the owner of this course");
        }
    }
}
