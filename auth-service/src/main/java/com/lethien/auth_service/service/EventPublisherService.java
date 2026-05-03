package com.lethien.auth_service.service;

import com.lethien.common_lib.constant.KafkaTopics;
import com.lethien.common_lib.event.AccountCreatedEvent;
import com.lethien.common_lib.event.EmailChangeCancelledEvent;
import com.lethien.common_lib.event.EmailChangeConfirmedEvent;
import com.lethien.common_lib.event.EmailChangeRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * EventPublisherService — publishes domain events to Kafka topics.
 *
 * Fire-and-forget with async callback logging.
 * If Kafka is unavailable after DB commit, the event will be lost.
 * For production, consider the Outbox Pattern to guarantee delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish AccountCreatedEvent to Kafka.
     *
     * Partition key = accountId → all events for the same account
     * go to the same partition, preserving order.
     *
     * @param event the event to publish
     */
    public void publishAccountCreatedEvent(AccountCreatedEvent event) {
        // ✅ Dùng KafkaTopics từ lib-common, không dùng KafkaTopicConfig
        String topic = KafkaTopics.ACCOUNT_CREATED;
        String key = event.getAccountId().toString();

        log.info("Publishing AccountCreatedEvent: accountId={}, email={}", event.getEventId(), event.getEmail());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("AccountCreatedEvent published: topic={}, partition={}, offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                // Event lost — account already saved in DB but User Service won't receive it.
                // Manual recovery needed: query accounts without user profiles and re-publish.

                log.error("CRITICAL — Failed to publish AccountCreatedEvent: accountId={}, error={}. " +
                                "User profile will NOT be created. Manual intervention required.",
                        event.getAccountId(), ex.getMessage(), ex);
            }
        });
    }

    public void publishEmailChangeRequestedEvent (EmailChangeRequestedEvent event) {
        try {
            kafkaTemplate.send(KafkaTopics.EMAIL_CHANGE_REQUESTED, event.getAccountId().toString(), event);
            log.info("Published EmailChangeRequestedEvent: accountId={}", event.getAccountId());
        } catch (Exception e) {
            log.error("Failed to publish EmailChangeRequestedEvent: accountId={}", event.getAccountId(), e);
        }
    }

    public void publishEmailChangeConfirmedEvent (EmailChangeConfirmedEvent event) {
        try {
            kafkaTemplate.send(KafkaTopics.EMAIL_CHANGE_CONFIRMED, event.getAccountId().toString(), event);
            log.info("Published EmailChangeConfirmedEvent: accountId={}", event.getAccountId());
        } catch (Exception e) {
            log.error("Failed to publish EmailChangeConfirmedEvent: accountId={}", event.getAccountId(), e);
        }
    }

    public void publishEmailChangeCancelledEvent(EmailChangeCancelledEvent event) {
        try {
            kafkaTemplate.send(KafkaTopics.EMAIL_CHANGE_CANCELLED, event.getAccountId().toString(), event);
            log.info("Published EmailChangeCancelledEvent: accountId={}", event.getAccountId());
        } catch (Exception e) {
            log.error("Failed to publish EmailChangeCancelledEvent: accountId={}", event.getAccountId(), e);
        }
    }
}
