package com.lethien.course_service.repository;

import com.lethien.course_service.entity.LessonProgress;
import com.lethien.course_service.entity.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);

    boolean existsByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);

    List<LessonProgress> findAllByEnrollmentId(UUID enrollmentId);

    List<LessonProgress> findAllByEnrollmentIdAndStatus(UUID enrollmentId, ProgressStatus status);

    /**
     * Count completed lessons in an enrollment.
     * Used to check if course is fully completed after each lesson update.
     */

    @Query("""
            SELECT COUNT(lp) FROM LessonProgress lp
            WHERE lp.enrollment.id = :enrollmentId
            AND lp.status = 'COMPLETED'
            """)
    long countCompletedByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);

    /**
     * Find progress for all lessons in a section — used to render section progress bar.
     */
    @Query("""
            SELECT lp FROM LessonProgress lp
            JOIN lp.lesson l
            WHERE lp.enrollment.id = :enrollmentId
            AND l.section.id = :sectionId
            ORDER BY l.orderIndex ASC
            """)
    List<LessonProgress> findByEnrollmentIdAndSectionId(
            @Param("enrollmentId") UUID enrollmentId,
            @Param("sectionId")    UUID sectionId
    );
}
