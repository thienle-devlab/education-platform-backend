package com.lethien.user_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * KafkaConsumerConfig — only configures what application.yml cannot:
 * concurrency and manual acknowledgment mode.
 *
 * All other consumer settings (bootstrap-servers, group-id, deserializers,
 * offset reset, trusted packages) are handled by Spring Boot auto-configuration
 * via application.yml → no need to redeclare them here.
 */

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    /**
     * Number of concurrent consumers — should match partition count.
     * Externalized to application.yml so it can differ per environment.
     */
    @Value("${kafka.consumer.concurrency:3}")
    private int concurrency;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // 3 concurrent consumers = 1 per partition (topic has 3 partitions)
        factory.setConcurrency(concurrency);

        // MANUAL_IMMEDIATE: offset committed only after Acknowledgment.acknowledge() is called.
        // Prevents message loss if the consumer crashes before processing completes.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
