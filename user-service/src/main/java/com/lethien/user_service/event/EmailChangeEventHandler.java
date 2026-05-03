package com.lethien.user_service.event;

import com.lethien.common_lib.constant.KafkaTopics;
import com.lethien.common_lib.event.EmailChangeCancelledEvent;
import com.lethien.common_lib.event.EmailChangeConfirmedEvent;
import com.lethien.common_lib.event.EmailChangeRequestedEvent;
import com.lethien.user_service.entity.ProcessedEvent;
import com.lethien.user_service.repository.ProcessedEventRepository;
import com.lethien.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailChangeEventHandler {

    private final UserService userService;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Chỉ log — email chưa được xác nhận, không sync DB
     */

    @KafkaListener(
            topics = KafkaTopics.EMAIL_CHANGE_REQUESTED,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void  handleRequested (EmailChangeRequestedEvent event) {
        log.info("Email change requested: accountId={}, requestedEmail={}",
                event.getAccountId(), event.getRequestedEmail());
        // Không update DB — chờ CONFIRMED
    }

    /**
     * Sync email thực sự khi user đã xác nhận
     */
    @KafkaListener(
            topics = KafkaTopics.EMAIL_CHANGE_CONFIRMED,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void handleConfirmed(EmailChangeConfirmedEvent event) {
        log.info("Email change confirmed: accountId={}", event.getAccountId());

        // 1. Validate payload
        if (!event.isValid()) {
            log.warn("Invalid EmailChangeConfirmedEvent, skipping. eventId={}", event.getEventId());
            return;
        }

        // 2. Idempotency check — tránh xử lý lại nếu Kafka deliver lại
        String key = event.idempotencyKey();
        if (processedEventRepository.existsByIdempotencyKey(key)) {
            log.info("Duplicate event, skipping. key={}", key);
            return;
        }

        // Delegate xuống UserService — đúng pattern hiện tại
        userService.syncEmail(event.getAccountId(), event.getNewEmail());

        processedEventRepository.save(ProcessedEvent.builder()
                .idempotencyKey(key)
                .processedAt(ZonedDateTime.now())
                .build());
    }

    /**
     * Chỉ log — không cần rollback vì REQUESTED không sync DB
     */
    @KafkaListener(
            topics = KafkaTopics.EMAIL_CHANGE_CANCELLED,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleCancelled(EmailChangeCancelledEvent event) {
        log.info("Email change cancelled: accountId={}, reason={}",
                event.getAccountId(), event.getReason());
    }
}
