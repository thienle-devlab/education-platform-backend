package com.lethien.course_service.service;

import com.lethien.common_lib.dto.PageResponse;
import com.lethien.course_service.dto.CreateReviewRequest;
import com.lethien.course_service.dto.ReviewResponse;
import com.lethien.course_service.entity.Course;
import com.lethien.course_service.entity.CourseReview;
import com.lethien.course_service.entity.Enrollment;
import com.lethien.course_service.entity.EnrollmentStatus;
import com.lethien.course_service.exception.AlreadyReviewedException;
import com.lethien.course_service.exception.CourseNotFoundException;
import com.lethien.course_service.exception.NotEligibleToReviewException;
import com.lethien.course_service.exception.ReviewNotFoundException;
import com.lethien.course_service.repository.CourseReviewRepository;
import com.lethien.course_service.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final CourseReviewRepository reviewRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;

    // ============================================
    // CREATE
    // ============================================

    /**
     * Tạo review cho khóa học.
     *
     * Điều kiện:
     *   1. Course phải PUBLISHED
     *   2. User phải có enrollment ACTIVE hoặc COMPLETED
     *   3. User chưa review course này trước đó
     *
     * Sau khi tạo, trigger trigger_rating_update trong DB
     * sẽ tự động cập nhật courses.average_rating.
     *
     * @param courseId target course
     * @param userId   userId từ User Service (không phải accountId)
     * @param request  rating + comment
     */
    @Transactional
    public ReviewResponse createReview(UUID courseId, UUID userId, CreateReviewRequest request) {

        log.info("Creating review: courseId={}, userId={}", courseId, userId);

        // 1️⃣ Course phải tồn tại và PUBLISHED
        Course course = courseService.findCourseById(courseId);
        if (!course.isPublished()) {
            throw new CourseNotFoundException("Course is not available for review: " + courseId);
        }

        // 2️⃣ User phải có enrollment ACTIVE hoặc COMPLETED
        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new NotEligibleToReviewException(
                        "You must be enrolled in this course to leave a review"));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE
                && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new NotEligibleToReviewException(
                    "Only active or completed enrollments can leave a review");
        }

        // 3️⃣ Chưa review trước đó
        if (reviewRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new AlreadyReviewedException(
                    "You have already reviewed this course");
        }

        // 4️⃣ Tạo review
        // isVerifiedPurchase = true vì user đang enrolled
        CourseReview review = CourseReview.builder()
                .userId(userId)
                .course(course)
                .rating(request.getRating())
                .comment(request.getComment())
                .isVerifiedPurchase(true)
                .build();

        CourseReview saved = reviewRepository.save(review);

        log.info("Review created: reviewId={}, courseId={}, userId={}, rating={}",
                saved.getId(), courseId, userId, saved.getRating());

        return ReviewResponse.from(saved);
    }

    // ============================================
    // READ
    // ============================================

    /**
     * Lấy danh sách review của một course — public endpoint.
     * Chỉ trả về verified purchase reviews.
     * Used in: GET /courses/{id}/reviews
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getCourseReviews(UUID courseId, Pageable pageable) {

        log.info("Getting reviews: courseId={}", courseId);

        // Verify course tồn tại
        courseService.findCourseById(courseId);

        Page<CourseReview> page = reviewRepository
                .findAllByCourseIdAndIsVerifiedPurchaseTrueOrderByCreatedAtDesc(courseId, pageable);

        return PageResponse.of(page.map(ReviewResponse::from));
    }

    /**
     * Lấy review của user cho một course cụ thể.
     * Dùng để check user đã review chưa trước khi hiển thị form.
     * Used in: GET /courses/{id}/reviews/me
     */
    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(UUID courseId, UUID userId) {

        log.info("Getting my review: courseId={}, userId={}", courseId, userId);

        CourseReview review = reviewRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found for courseId=" + courseId
                        + ", userId=" + userId));

        return ReviewResponse.from(review);
    }

    // ============================================
    // DELETE
    // ============================================

    /**
     * Xóa review — chỉ owner mới được xóa.
     * Sau khi xóa, trigger trigger_rating_update tự cập nhật average_rating.
     * Used in: DELETE /courses/{id}/reviews/me
     */
    @Transactional
    public void deleteReview(UUID courseId, UUID userId) {

        log.info("Deleting review: courseId={}, userId={}", courseId, userId);

        CourseReview review = reviewRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found for courseId=" + courseId
                        + ", userId=" + userId));

        reviewRepository.delete(review);

        log.info("Review deleted: courseId={}, userId={}", courseId, userId);
    }
}
