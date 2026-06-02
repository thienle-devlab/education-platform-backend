package com.lethien.course_service.repository;

import com.lethien.course_service.entity.SubmissionStatus;
import com.lethien.course_service.entity.WritingSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, UUID> {

    List<WritingSubmission> findAllByUserIdAndExerciseIdOrderByAttemptNumberDesc(
            UUID userId, UUID exerciseId);

    Optional<WritingSubmission> findByUserIdAndExerciseIdAndAttemptNumber(
            UUID userId, UUID exerciseId, Integer attemptNumber);

    /**
     * Find PENDING writing submissions for the AI grading queue.
     * Ordered by submitted_at ASC — FIFO processing.
     * Leverages partial index: idx_writing_pending.
     */
    @Query("""
        SELECT ws FROM WritingSubmission ws
        WHERE ws.status = 'PENDING'
        ORDER BY ws.submittedAt ASC
        """)
    Page<WritingSubmission> findPendingForGrading(Pageable pageable);

    long countByUserIdAndExerciseIdAndStatus(UUID userId, UUID exerciseId, SubmissionStatus status);
}
