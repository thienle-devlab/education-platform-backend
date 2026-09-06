package com.lethien.course_service.listener;

import com.lethien.common_lib.constant.KafkaTopics;
import com.lethien.common_lib.event.SubmissionGradedEvent;
import com.lethien.course_service.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * SubmissionGradedEventListener — consumes grading results from AI Service.
 *
 * Published by: AI Service after grading SPEAKING or WRITING submissions.
 * Topic: "course.submission.graded"
 *
 * Flow on receive:
 *   1. Validate event
 *   2. Delegate to SubmissionService.handleGradingResult()
 *      → updates SpeakingSubmission / WritingSubmission
 *      → syncs score to ExerciseAttempt
 *      → triggers LessonProgress recalculation
 *   3. Acknowledge offset after successful processing
 *
 * On failure: message is NOT acknowledged → Kafka retries.
 * On invalid event (poison pill): acknowledged to unblock partition.
 *
 * NOTE: AI Service chưa được implement. Khi AI Service sẵn sàng,
 * nó cần publish SubmissionGradedEvent với eventType = "SUBMISSION_GRADED"
 * lên topic "course.submission.graded".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionGradedEventListener {

    private final SubmissionService submissionService;

    @KafkaListener(
            topics = KafkaTopics.COURSE_SUBMISSION_GRADED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSubmissionGraded(
            @Payload SubmissionGradedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
            ) {
        log.info("Received SubmissionGradedEvent: submissionId={}, type={}, success={}, " +
                        "partition={}, offset={}",
                event.getSubmissionId(), event.getSubmissionType(),
                event.getSuccess(), partition, offset);

        // 1️⃣ Validate event — skip poison pill
        if (!event.isValid()) {
            log.error("Invalid SubmissionGradedEvent — missing required fields: eventId={}. " +
                    "Acknowledging to skip poison pill.", event.getEventId());

            acknowledgment.acknowledge();
            return;
        }

        try {
            // 2️⃣ Delegate to SubmissionService
            submissionService.handleGradingResult(event);

            // 3️⃣ Commit offset after successful processing
            acknowledgment.acknowledge();

            log.info("SubmissionGradedEvent processed: submissionId={}, partition={}, offset={}",
                    event.getSubmissionId(), partition, offset);
        } catch (DataIntegrityViolationException e) {
            // Duplicate processing — safe to acknowledge and skip
            log.warn("Duplicate grading result detected — acknowledging to stop retry: " +
                    "submissionId={}", event.getSubmissionId());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            // Unexpected error — do NOT acknowledge → Kafka will retry
            log.error("Failed to process SubmissionGradedEvent: submissionId={}, error={}",
                    event.getSubmissionId(), e.getMessage(), e);
        }
    }
}
