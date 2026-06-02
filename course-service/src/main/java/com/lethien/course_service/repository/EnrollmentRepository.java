package com.lethien.course_service.repository;

import com.lethien.course_service.entity.Enrollment;
import com.lethien.course_service.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    // ============================================
    // FIND BY USER / COURSE
    // ============================================

    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    Page<Enrollment> findAllByUserId(UUID userId, Pageable pageable);

    Page<Enrollment> findAllByUserIdAndStatus(UUID userId, EnrollmentStatus status, Pageable pageable);

    Page<Enrollment> findAllByCourseId(UUID courseId, Pageable pageable);

    // ============================================
    // STATISTICS
    // ============================================

    long countByCourseId(UUID courseId);

    long countByCourseIdAndStatus(UUID courseId, EnrollmentStatus status);

    /**
     * Count enrollments in a course by status — used for instructor dashboard.
     */
    @Query("""
        SELECT COUNT(e) FROM Enrollment e
        JOIN e.course c
        WHERE c.instructorId = :instructorId
          AND e.status = :status
        """)
    long countByInstructorIdAndStatus(
            @Param("instructorId") UUID instructorId,
            @Param("status") EnrollmentStatus status
    );
}
