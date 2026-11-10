package com.lethien.course_service.service;

import com.lethien.course_service.entity.Course;
import com.lethien.course_service.entity.EnrollmentStatus;
import com.lethien.course_service.exception.CourseNotFoundException;
import com.lethien.course_service.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * CourseAccessService — xác định "người đang xem" có quyền gì với một course.
 *
 *   OWNER    : instructor sở hữu course              → thấy toàn bộ nội dung
 *   ENROLLED : học viên có enrollment ACTIVE/COMPLETED → thấy toàn bộ nội dung
 *   PUBLIC   : khách, hoặc đã đăng nhập nhưng chưa enroll → chỉ thấy cấu trúc,
 *              và nội dung của lesson có isPreview = true
 *
 * userId = null nghĩa là khách (không có JWT hợp lệ).
 */
@Service
@RequiredArgsConstructor
public class CourseAccessService {

    public enum ViewerAccess {
        OWNER, ENROLLED, PUBLIC;

        public boolean canViewAllContent() {
            return this != PUBLIC;
        }
    }

    private final EnrollmentRepository enrollmentRepository;

    public ViewerAccess resolve(Course course, UUID userId) {
        if (userId == null) {
            return ViewerAccess.PUBLIC;
        }
        if (course.getInstructorId().equals(userId)) {
            return ViewerAccess.OWNER;
        }
        boolean enrolled = enrollmentRepository
                .findByUserIdAndCourseId(userId, course.getId())
                .map(e -> e.getStatus() == EnrollmentStatus.ACTIVE
                        || e.getStatus() == EnrollmentStatus.COMPLETED)
                .orElse(false);
        return enrolled ? ViewerAccess.ENROLLED : ViewerAccess.PUBLIC;
    }

    /**
     * Dùng cho các endpoint đọc công khai: người xem PUBLIC không được thấy
     * course chưa PUBLISHED (DRAFT/ARCHIVED) — trả 404 để không lộ sự tồn tại.
     * Owner và học viên đã enroll vẫn xem được (ví dụ course đã ARCHIVED).
     */
    public ViewerAccess resolveForReading(Course course, UUID userId) {
        ViewerAccess access = resolve(course, userId);
        if (access == ViewerAccess.PUBLIC && !course.isPublished()) {
            throw new CourseNotFoundException("Course not found: " + course.getId());
        }
        return access;
    }
}
