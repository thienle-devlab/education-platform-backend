package com.lethien.course_service.config;

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
 * offset reset, trusted packages, type.mapping) are handled by Spring Boot
 * auto-configuration via application.yml.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${kafka.consumer.concurrency:3}")
    private int concurrency;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // 3 concurrent consumers = 1 per partition
        factory.setConcurrency(concurrency);

        // MANUAL_IMMEDIATE: offset committed only after Acknowledgment.acknowledge() is called
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
