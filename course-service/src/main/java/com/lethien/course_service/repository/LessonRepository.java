package com.lethien.course_service.repository;

import com.lethien.course_service.entity.Lesson;
import com.lethien.course_service.entity.LessonType;
import com.lethien.course_service.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findAllBySectionIdOrderByOrderIndexAsc(UUID sectionId);

    List<Lesson> findAllBySectionIdAndLessonType(UUID sectionId, LessonType lessonType);

    boolean existsBySectionIdAndOrderIndex(UUID sectionId, Integer orderIndex);

    /**
     * Find max order_index in a section — used when appending a new lesson.
     */
    @Query("SELECT MAX(l.orderIndex) FROM Lesson l WHERE l.section.id = :sectionId")
    Optional<Integer> findMaxOrderIndexBySectionId(@Param("sectionId") UUID sectionId);

    /**
     * Get all preview lessons of a course (no enrollment needed).
     */
    @Query("""
            SELECT l FROM Lesson l
            JOIN l.section s
            WHERE s.course.id = :courseId AND l.isPreview = true
            ORDER BY s.orderIndex, l.orderIndex
            """)
    List<Lesson> findPreviewLessonsByCourseId(@Param("courseId") UUID courseId);

    long countBySectionId(UUID sectionId);
}
