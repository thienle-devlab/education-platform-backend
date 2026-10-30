package com.lethien.course_service.service;

import com.lethien.common_lib.dto.PageResponse;
import com.lethien.course_service.dto.CourseResponse;
import com.lethien.course_service.dto.CourseSummaryResponse;
import com.lethien.course_service.dto.CreateCourseRequest;
import com.lethien.course_service.dto.UpdateCourseRequest;
import com.lethien.course_service.entity.Course;
import com.lethien.course_service.entity.CourseLevel;
import com.lethien.course_service.entity.SkillType;
import com.lethien.course_service.exception.*;
import com.lethien.course_service.repository.CourseRepository;
import com.lethien.course_service.repository.CourseSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;

    // ============================================
    // CREATE
    // ============================================

    /**
     * Create a new course in DRAFT status.
     * Only instructors can create courses — role check done at controller level
     * via UserServiceClient (JWT chưa có role claim).
     *
     * @param request course creation payload
     * @return CourseResponse of the created course
     */
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        log.info("Creating course: title={}, instructorId={}", request.getTitle(), request.getInstructorId());

        // 1️⃣ Slug uniqueness check
        if (courseRepository.existsBySlug(request.getSlug())) {
            throw new SlugAlreadyExistsException("Slug already exists: " + request.getSlug());
        }

        // 2️⃣ Build entity — status=DRAFT by default
        Course course = Course.builder()
                .instructorId(request.getInstructorId())
                .title(request.getTitle())
                .slug(request.getSlug())
                .description(request.getDescription())
                .skillType(request.getSkillType())
                .level(request.getLevel() != null ? request.getLevel() : CourseLevel.BEGINNER)
                .thumbnailUrl(request.getThumbnailUrl())
                .trailerUrl(request.getTrailerUrl())
                .price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
                .durationHours(request.getDurationHours())
                .build();

        // 3️⃣ Add tags if provided
        if (request.getTags() != null) {
            request.getTags().forEach(course::addTag);
        }

        // 4️⃣ Save
        Course saved = courseRepository.save(course);

        log.info("Course created: id={}, slug={}, instructorId={}",
                saved.getId(), saved.getSlug(), saved.getInstructorId());

        return CourseResponse.from(saved);
    }

    // ============================================
    // READ
    // ============================================

    /**
     * Get full course detail by ID.
     * Returns any status — filtering by PUBLISHED happens in the catalog endpoint.
     */
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(UUID courseId) {
        log.info("Getting course by id={}", courseId);

        Course course = findCourseById(courseId);
        return CourseResponse.from(course);
    }

    /**
     * Get course by slug — used for SEO-friendly URLs.
     * Only returns PUBLISHED courses (public-facing).
     */
    @Transactional(readOnly = true)
    public CourseResponse getCourseBySlug(String slug) {
        log.info("Getting course by slug={}", slug);

        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new CourseNotFoundException("Course not found: " + slug));

        if (!course.isPublished()) {
            throw new CourseNotFoundException("Course not found: " + slug);
        }

        return CourseResponse.from(course);
    }

    /**
     * Get published course catalog with optional filters.
     * Public endpoint — no auth required.
     *
     * @param skillType filter by skill (nullable)
     * @param level     filter by level (nullable)
     * @param maxPrice  filter by max price (nullable)
     * @param pageable  pagination + sorting
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> getPublishedCourses(
            SkillType skillType,
            CourseLevel level,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        log.info("Getting published courses: skillType={}, level={}, maxPrice={}",
                skillType, level, maxPrice);

        Specification<Course> spec = Specification
                .where(CourseSpecifications.published())
                .and(CourseSpecifications.hasSkillType(skillType))
                .and(CourseSpecifications.hasLevel(level))
                .and(CourseSpecifications.priceAtMost(maxPrice));

        Page<Course> page = courseRepository.findAll(spec, pageable);

        Page<CourseSummaryResponse> mapped = page.map(CourseSummaryResponse::from);

        return PageResponse.of(mapped);
    }

    /**
     * Search published courses by keyword.
     * Uses full-text search for long queries (>= 3 chars), LIKE for short.
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> searchCourses(String keyword, Pageable pageable) {

        log.info("Searching courses: keyword={}", keyword);

        Page<Course> page;
        if (keyword == null || keyword.trim().length() < 3) {
            page = courseRepository.searchByKeyword(keyword, pageable);
        } else {
            page = courseRepository.fullTextSearch(keyword.trim(), pageable);
        }

        return PageResponse.of(page.map(CourseSummaryResponse::from));
    }

    /**
     * Get all courses by instructor — used in instructor dashboard.
     * Returns all statuses (DRAFT, PUBLISHED, ARCHIVED).
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> getCoursesByInstructor(UUID instructorId, Pageable pageable) {

        log.info("Getting courses by instructorId={}", instructorId);

        Page<Course> page = courseRepository.findAllByInstructorId(instructorId, pageable);

        return PageResponse.of(page.map(CourseSummaryResponse::from));
    }

    // ============================================
    // UPDATE
    // ============================================

    /**
     * Update course metadata — partial update (null fields are ignored).
     * Only the course owner (instructorId) can update.
     *
     * @param courseId  target course
     * @param instructorId accountId from JWT — used to verify ownership
     * @param request   partial update payload
     */
    @Transactional
    public CourseResponse updateCourse(UUID courseId, UUID instructorId, UpdateCourseRequest request) {

//        log.info("Updating course: courseId={}, accountId={}", courseId, accountId);
        log.info("Updating course: courseId={}, instructorId={}", courseId, instructorId);

        Course course = findCourseById(courseId);

        // Ownership check — accountId in JWT must match instructorId
        // NOTE: instructorId = userId from User Service (not accountId from Auth Service)
        // Inter-service call needed here if we want strict check.
        // For now: pass instructorId explicitly in request and verify.

//        verifyOwnership(course, request.getInstructorId());
        verifyOwnership(course, instructorId);

        // Partial update — only non-null fields
        if (request.getTitle() != null)
            course.setTitle(request.getTitle());
        if (request.getDescription() != null)
            course.setDescription(request.getDescription());
        if (request.getLevel() != null)
            course.setLevel(request.getLevel());
        if (request.getThumbnailUrl() != null)
            course.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getTrailerUrl() != null)
            course.setTrailerUrl(request.getTrailerUrl());
        if (request.getPrice() != null)
            course.setPrice(request.getPrice());
        if (request.getDurationHours() != null)
            course.setDurationHours(request.getDurationHours());

        // Tags — replace all if provided
        if (request.getTags() != null) {
            course.getTags().clear();
            request.getTags().forEach(course::addTag);
        }

        Course updated = courseRepository.save(course);

        log.info("Course updated: courseId={}", updated.getId());

        return CourseResponse.from(updated);
    }

    /**
     * Publish a DRAFT course — changes status to PUBLISHED and sets publishedAt.
     * Only the course owner can publish.
     */
    @Transactional
    public CourseResponse publishCourse(UUID courseId, UUID instructorId) {

        log.info("Publishing course: courseId={}, instructorId={}", courseId, instructorId);

        Course course = findCourseById(courseId);
        verifyOwnership(course, instructorId);

        if (course.isPublished()) {
            throw new CourseAlreadyPublishedException("Course is already published: " + courseId);
        }

        if (course.getTotalLessons() == 0) {
            throw new CoursePublishException("Cannot publish a course with no lessons: " + courseId);
        }

        course.publish();
        Course updated = courseRepository.save(course);

        log.info("Course published: courseId={}, publishedAt={}", updated.getId(), updated.getPublishedAt());

        return CourseResponse.from(updated);
    }

    /**
     * Archive a PUBLISHED course — changes status to ARCHIVED.
     * Existing enrollments are not affected.
     */
    @Transactional
    public CourseResponse archiveCourse(UUID courseId, UUID instructorId) {

        log.info("Archiving course: courseId={}, instructorId={}", courseId, instructorId);

        Course course = findCourseById(courseId);
        verifyOwnership(course, instructorId);

        course.archive();
        Course updated = courseRepository.save(course);

        log.info("Course archived: courseId={}", updated.getId());

        return CourseResponse.from(course);
    }

    // ============================================
    // DELETE
    // ============================================

    /**
     * Delete a DRAFT course.
     * PUBLISHED/ARCHIVED courses cannot be deleted — must be archived first.
     */
    @Transactional
    public void deleteCourse(UUID courseId, UUID instructorId) {

        log.info("Deleting course: courseId={}, instructorId={}", courseId, instructorId);

        Course course = findCourseById(courseId);
        verifyOwnership(course, instructorId);
        if (course.isPublished()) {
            throw new CoursePublishException("Cannot delete a published course. Archive it first: " + courseId);
        }

        courseRepository.delete(course);

        log.info("Course deleted: courseId={}", courseId);
    }

    // ============================================
    // INTERNAL HELPERS
    // ============================================

    /**
     * Find course by ID or throw CourseNotFoundException.
     */
    public Course findCourseById(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found: " + courseId));
    }

    /**
     * Verify that the given instructorId matches the course owner.
     */
//    private void verifyOwnership(Course course, UUID instructorId) {
//        if (!course.getInstructorId().equals(instructorId)) {
//            throw new CourseAccessDeniedException("Access denied: you are not the owner of this course");
//        }
//    }

    private void verifyOwnership(Course course, UUID instructorId) {
        if (instructorId == null
                || !instructorId.equals(course.getInstructorId())) {
            throw new CourseAccessDeniedException(
                    "Access denied: you are not the owner of this course"
            );
        }
    }

    /**
     * Provides reusable specifications for filtering courses.
     * These specifications can be combined dynamically to build course queries.
     */


}
