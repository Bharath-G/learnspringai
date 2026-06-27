package com.neobank.notification.config;

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

        // Retry 3 times: 1s → 2s → 4s then send to DLT
        var backoff = new ExponentialBackOff(1_000L, 2.0);
        backoff.setMaxAttempts(3);

        var handler = new DefaultErrorHandler(recoverer, backoff);

        handler.setRetryListeners((records, ex, deliveryAttempt) ->
                log.warn("Retry attempt {} for topic={} offset={}",
                        deliveryAttempt,
                        records.topic(),
                        records.offset()));

        return handler;
    }
}