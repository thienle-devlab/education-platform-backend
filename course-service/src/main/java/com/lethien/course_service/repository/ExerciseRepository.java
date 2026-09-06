package com.lethien.course_service.repository;

import com.lethien.course_service.entity.Exercise;
import com.lethien.course_service.entity.ExerciseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findAllByLessonIdOrderByOrderIndexAsc(UUID lessonId);

    List<Exercise> findAllByLessonIdAndExerciseType(UUID lessonId, ExerciseType exerciseType);

    boolean existsByLessonIdAndOrderIndex(UUID lessonId, Integer orderIndex);

    /**
     * Find max order_index in a lesson — used when appending a new exercise.
     */
    @Query("SELECT MAX(e.orderIndex) FROM Exercise e WHERE e.lesson.id = :lessonId")
    Optional<Integer> findMaxOrderIndexByLessonId(@Param("lessonId") UUID lessonId);

    long countByLessonId(UUID lessonId);

    /**
     * Find all exercises in a lesson that require AI grading.
     * Used by SubmissionService to validate exercise type before creating submission.
     */
    @Query("""
            SELECT e FROM Exercise e
            WHERE e.lesson.id = :lessonId
            AND e.exerciseType IN ('ESSAY', 'SPEAKING_RECORD', 'SPEAKING_REPEAT')
            ORDER BY e.orderIndex ASC
            """)
    List<Exercise> findAiGradedExercisesByLessonId(@Param("lessonId") UUID lessonId);
}
