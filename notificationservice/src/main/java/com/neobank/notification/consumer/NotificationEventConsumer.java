package com.neobank.notification.consumer;

import com.neobank.events.*;
import com.neobank.notification.constant.Constants;
import com.neobank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "account-events",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAccountEvent(
            @Payload AccountCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.debug("Received AccountCreatedEvent partition={} offset={} " +
                Constants.CORRELATION_ID, partition, offset, event.getCorrelationId());

        try {
            notificationService.notifyAccountCreated(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process AccountCreatedEvent " +
                    Constants.CORRELATION_ID, event.getCorrelationId(), e);
        }
    }

    @KafkaListener(
            topics = "transaction-completed",
            groupId = "notification-service"
    )
    public void handleTransactionCompleted(
            @Payload TransactionCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.debug("Received TransactionCompletedEvent partition={} " +
                        "offset={} correlationId={}",
                partition, offset, event.getCorrelationId());

        try {
            notificationService.notifyTransactionCompleted(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process TransactionCompletedEvent " +
                    Constants.CORRELATION_ID, event.getCorrelationId(), e);
        }
    }

    @KafkaListener(
            topics = "transaction-failed",
            groupId = "notification-service"
    )
    public void handleTransactionFailed(
            @Payload TransactionFailedEvent event,
            Acknowledgment ack) {

        try {
            notificationService.notifyTransactionFailed(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process TransactionFailedEvent " +
                    Constants.CORRELATION_ID, event.getCorrelationId(), e);
        }
    }

    @KafkaListener(
            topics = "fraud-alerts",
            groupId = "notification-service"
    )
    public void handleFraudAlert(
            @Payload TransactionFlaggedEvent event,
            Acknowledgment ack) {

        try {
            notificationService.notifyTransactionFlagged(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process TransactionFlaggedEvent " +
                    Constants.CORRELATION_ID, event.getCorrelationId(), e);
        }
    }

    @KafkaListener(
            topics = "account-frozen",
            groupId = "notification-service"
    )
    public void handleAccountFrozen(
            @Payload AccountFrozenEvent event,
            Acknowledgment ack) {

        try {
            notificationService.notifyAccountFrozen(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process AccountFrozenEvent " +
                    Constants.CORRELATION_ID, event.getCorrelationId(), e);
        }
    }

    @KafkaListener(
            topics = "transaction-blocked",
            groupId = "notification-service"
    )
    public void handleTransactionBlocked(
            @Payload TransactionBlockedEvent event,
            Acknowledgment ack) {

        try {
            notificationService.notifyTransactionBlocked(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process TransactionBlockedEvent " +
                    Constants.CORRELATION_ID, event.getCorrelationId(), e);
        }
    }

    // DLT handler — catches anything that failed all retries
    @KafkaListener(
            topics = {
                    "account-events.DLT",
                    "transaction-completed.DLT",
                    "transaction-failed.DLT",
                    "fraud-alerts.DLT",
                    "account-frozen.DLT",
                    "transaction-blocked.DLT"
            },
            groupId = "notification-dlt-handler"
    )
    public void handleDlt(
            @Payload String rawEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage,
            Acknowledgment ack) {

        log.error("DLT: notification failed permanently");
        log.error("  Topic : {}", topic);
        log.error("  Error : {}", errorMessage);
        log.error("  Event : {}", rawEvent);
        // In production: write to error DB, alert on-call team
        ack.acknowledge();
    }
}