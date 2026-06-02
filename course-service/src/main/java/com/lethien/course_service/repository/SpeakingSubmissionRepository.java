package com.lethien.course_service.repository;

import com.lethien.course_service.entity.SpeakingSubmission;
import com.lethien.course_service.entity.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpeakingSubmissionRepository extends JpaRepository<SpeakingSubmission, UUID> {

    List<SpeakingSubmission> findAllByUserIdAndExerciseIdOrderByAttemptNumberDesc(
            UUID userId, UUID exerciseId);

    Optional<SpeakingSubmission> findByUserIdAndExerciseIdAndAttemptNumber(
            UUID userId, UUID exerciseId, Integer attemptNumber);

    /**
     * Find PENDING speaking submissions for the AI grading queue.
     * Ordered by submitted_at ASC — FIFO processing.
     * Leverages partial index: idx_speaking_pending.
     */
    @Query("""
        SELECT ss FROM SpeakingSubmission ss
        WHERE ss.status = 'PENDING'
        ORDER BY ss.submittedAt ASC
        """)
    Page<SpeakingSubmission> findPendingForGrading(Pageable pageable);

    long countByUserIdAndExerciseIdAndStatus(UUID userId, UUID exerciseId, SubmissionStatus status);
}
