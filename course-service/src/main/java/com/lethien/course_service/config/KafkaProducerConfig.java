package com.lethien.course_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * KafkaProducerConfig — expose KafkaTemplate bean cho các service publish event.
 *
 * Serializer settings (JsonSerializer, add.type.headers=false) đã được
 * config trong application.yml — không cần redeclare ở đây.
 *
 * Consumers:
 *   - EnrollmentService  → publish EnrollmentCreatedEvent
 *   - SubmissionService  → publish SpeakingSubmissionCreatedEvent, WritingSubmissionCreatedEvent
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
