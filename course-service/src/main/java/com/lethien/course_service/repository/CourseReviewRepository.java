package com.lethien.course_service.repository;

import com.lethien.course_service.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {

    Optional<CourseReview> findByUserIdAndCourseId(UUID userId, UUID courseId);

    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    Page<CourseReview> findAllByCourseIdOrderByCreatedAtDesc(UUID courseId, Pageable pageable);

    /**
     * Find verified-purchase reviews only — useful for public display.
     */
    Page<CourseReview> findAllByCourseIdAndIsVerifiedPurchaseTrueOrderByCreatedAtDesc(UUID courseId, Pageable pageable);

    /**
     * Average rating for a course — used for validation; actual denormalization is done by trigger.
     */
    @Query("SELECT AVG(r.rating) FROM CourseReview r WHERE r.course.id = :courseId")
    Optional<Double> findAverageRatingByCourseId(@Param("courseId") UUID courseId);

    long countByCourseId(UUID courseId);
}
