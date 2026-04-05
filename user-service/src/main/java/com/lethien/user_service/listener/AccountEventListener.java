package com.lethien.user_service.listener;

import com.lethien.common_lib.constant.KafkaTopics;
import com.lethien.common_lib.event.AccountCreatedEvent;
import com.lethien.user_service.dto.CreateUserRequest;
import com.lethien.user_service.service.UserService;
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
 * AccountEventListener — consumes account domain events from Auth Service.
 *
 * Manual acknowledgment: offset is committed only after successful processing.
 * On failure: message is NOT acknowledged → Kafka retries delivery.
 * After max retries: message goes to Dead Letter Topic (DLT) if configured.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventListener {

    private final UserService userService;

    /**
     * Handle AccountCreatedEvent.
     *
     * Topic sourced from KafkaTopics (lib-common) — same constant used by
     * Auth Service producer, guaranteed to match.
     *
     * groupId from application.yml — not hardcoded here so it can be
     * overridden per environment without recompiling.
     */
    @KafkaListener(
            topics = KafkaTopics.ACCOUNT_CREATED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAccountCreated(
            @Payload AccountCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received AccountCreatedEvent: accountId={}, email={}, partition={}, offset={}",
                event.getAccountId(), event.getEmail(), partition, offset);

        // Basic validation before processing
        if (!event.isValid()) {
            log.error("Invalid AccountCreatedEvent received — missing required fields: {}. " +
                    "Acknowledging to skip poison pill message.", event.getEventId());

            // Acknowledge invalid message so it doesn't block the partition forever
            acknowledgment.acknowledge();
            return;
        }

        try {
            // Map event → CreateUserRequest
            // fullName from event — no firstName/lastName split here
            CreateUserRequest request = CreateUserRequest.builder()
                    .accountId(event.getAccountId())
                    .email(event.getEmail())
                    .fullName(event.getFullName())
                    .phoneNumber(event.getPhoneNumber())
                    .build();

            userService.createUser(request);

            // Commit offset only after successful processing
            acknowledgment.acknowledge();

            log.info("AccountCreatedEvent processed: accountId={}, partition={}, offset={}",
                    event.getAccountId(), partition, offset);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate user detected — acknowledge to stop retry: accountId={}",
                    event.getAccountId());
            acknowledgment.acknowledge(); // stop retry
        }
        catch (Exception e) {
            log.error("Unexpected error — retrying", e);
        }
    }
}
