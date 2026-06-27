package com.neobank.fraud.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(
            KafkaTemplate<String, Object> kafkaTemplate) {

        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // Retry 3 times with exponential backoff
        // AI calls can be slow — give more time between retries
        var backoff = new ExponentialBackOff(2_000L, 2.0); // 2s, 4s, 8s
        backoff.setMaxAttempts(3);

        var handler = new DefaultErrorHandler(recoverer, backoff);

        handler.setRetryListeners((records, ex, deliveryAttempt) ->
            log.warn("Fraud analysis retry attempt={} " +
                     "topic={} partition={} offset={}",
                deliveryAttempt,
                records.topic(),
                records.partition(),
                records.offset()));

        return handler;
    }
}