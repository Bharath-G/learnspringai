package com.neobank.fraud.config;

import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    // Topics this service PRODUCES to
    @Bean
    public KafkaAdmin.NewTopics fraudTopics() {
        return new KafkaAdmin.NewTopics(

            TopicBuilder.name("transaction-approved")
                .partitions(6)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")
                .build(),

            TopicBuilder.name("fraud-alerts")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 days
                .build(),

            TopicBuilder.name("account-frozen")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000")
                .build(),

            // DLT for failed fraud analyses
            TopicBuilder.name("transaction-initiated.DLT")
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000")
                .build()
        );
    }
}