package com.lethien.course_service.repository;

import com.lethien.course_service.entity.ExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, UUID> {

    List<ExerciseAttempt> findAllByUserIdAndExerciseIdOrderByAttemptNumberDesc(UUID userId, UUID exerciseId);

    Optional<ExerciseAttempt> findByUserIdAndExerciseIdAndAttemptNumber(UUID userId, UUID exerciseId, Integer attemptNumber);

    /**
     * Find the latest attempt for a user on a specific exercise.
     */
    @Query("""
            SELECT ea FROM ExerciseAttempt ea
            WHERE ea.userId = :userId AND ea.exercise.id = :exerciseId
            ORDER BY ea.attemptNumber DESC
            LIMIT 1
            """)
    Optional<ExerciseAttempt> findLatestAttempt(
            @Param("userId") UUID userId,
            @Param("exerciseId") UUID exerciseId
    );

    /**
     * Find the next attempt number for a user on a specific exercise.
     */
    @Query("""
        SELECT COALESCE(MAX(ea.attemptNumber), 0) + 1
        FROM ExerciseAttempt ea
        WHERE ea.userId = :userId AND ea.exercise.id = :exerciseId
        """)
    int findNextAttemptNumber(
            @Param("userId") UUID userId,
            @Param("exerciseId") UUID exerciseId
    );

    long countByUserIdAndExerciseId(UUID userId, UUID exerciseId);
}
