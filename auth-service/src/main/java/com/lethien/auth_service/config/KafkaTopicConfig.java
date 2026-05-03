package com.lethien.auth_service.config;

import com.lethien.common_lib.constant.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * KafkaTopicConfig — defines Kafka topics for Auth Service events.
 *
 * Topics are auto-created on startup if they don't exist.
 * If they already exist with different settings, Kafka will NOT
 * modify them — delete and recreate manually if settings need to change.
 *
 * Topic names are sourced from KafkaTopics (lib-common) so both
 * producer (Auth Service) and consumer (User Service) use the same constants.
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Number of partitions — controls parallelism.
     * Each consumer in a group can read from at most 1 partition,
     * so partitions = max concurrent consumers per topic.
     * Default: 3 (suitable for dev and small production load).
     */
    @Value("${kafka.topic.partitions:3}")
    private int partitions;

    /**
     * Replication factor — controls fault tolerance.
     * Dev/local: 1 (single broker, no replication).
     * Production: minimum 3 (tolerate 1 broker failure with min.insync.replicas=2).
     */
    @Value("${kafka.topic.replicas:1}")
    private short replicas;

    // ============================================
    // ACCOUNT EVENTS
    // ============================================

    /**
     * auth.account.created
     * Published when a new account is registered.
     * Consumed by: User Service (create User profile).
     *
     * retention.ms = 7 days — events older than 7 days are deleted.
     * Adjust to longer retention if downstream services may be offline longer.
     */
    @Bean
    public NewTopic accountCreatedTopic() {
        return TopicBuilder
                .name(KafkaTopics.ACCOUNT_CREATED)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000))
                .build();
    }

    /**
     * auth.account.updated
     * Published when account details change (email, status, password).
     * Consumed by: User Service (sync email changes).
     */
    @Bean
    public NewTopic accountUpdatedTopic() {
        return TopicBuilder
                .name(KafkaTopics.ACCOUNT_UPDATED)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000L))
                .build();
    }

    /**
     * auth.account.deleted
     * Published when an account is permanently deleted.
     * Consumed by: User Service (remove User profile and related data).
     *
     * Longer retention (30 days) — deletion events are critical for compliance,
     * downstream services must process them even after extended downtime.
     */
    @Bean
    public NewTopic accountDeletedTopic() {
        return TopicBuilder
                .name(KafkaTopics.ACCOUNT_DELETED)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(30 * 24 * 60 * 60 * 1000L))
                .build();
    }

    // ============================================
    // EMAIL CHANGE EVENTS
    // ============================================

    /**
     * auth.email.change.requested
     * Published khi user submit form đổi email — chưa xác nhận.
     * Consumed by: User Service (chỉ log, không sync DB).
     *
     * retention.ms = 1 day — event này chỉ có giá trị trong thời gian
     * token còn hiệu lực (24h), giữ lâu hơn không có ý nghĩa.
     */
    @Bean
    public NewTopic emailChangeRequestedTopic () {
        return TopicBuilder
                .name(KafkaTopics.EMAIL_CHANGE_REQUESTED)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(24 * 60 * 60 * 1000L))
                .build();
    }

    /**
     * auth.email.change.confirmed
     * Published khi user click link xác nhận — email chính thức được đổi.
     * Consumed by: User Service (sync email vào user_db).
     *
     * retention.ms = 7 days — đủ thời gian để User Service xử lý
     * kể cả khi bị downtime ngắn.
     */
    @Bean
    public NewTopic emailChangeConfirmedTopic () {
        return TopicBuilder
                .name(KafkaTopics.EMAIL_CHANGE_CONFIRMED)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000L))
                .build();
    }

    /**
     * auth.email.change.cancelled
     * Published khi user huỷ hoặc token hết hạn.
     * Consumed by: User Service (chỉ log, không cần rollback).
     *
     * retention.ms = 1 day — tương tự REQUESTED, không cần giữ lâu.
     */
    @Bean
    public NewTopic emailChangeCancelledTopic() {
        return TopicBuilder
                .name(KafkaTopics.EMAIL_CHANGE_CANCELLED)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(24 * 60 * 60 * 1000L))
                .build();
    }
}
