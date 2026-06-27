package com.neobank.fraud.consumer;

import com.neobank.events.TransactionInitiatedEvent;
import com.neobank.fraud.model.FraudAnalysisResult;
import com.neobank.fraud.producer.FraudEventProducer;
import com.neobank.fraud.service.FraudDetectionService;
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
public class TransactionEventConsumer {

    private final FraudDetectionService fraudDetectionService;
    private final FraudEventProducer fraudEventProducer;

    @KafkaListener(
        topics = "transaction-initiated",
        groupId = "fraud-detection-service",
        concurrency = "3"
    )
    public void handleTransactionInitiated(
            @Payload TransactionInitiatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Received TransactionInitiatedEvent " +
                 "transactionId={} amount={} partition={} offset={} " +
                 "correlationId={}",
            event.getTransactionId(),
            event.getAmount(),
            partition, offset,
            event.getCorrelationId());

        try {
            // Step 1 — run AI fraud analysis
            FraudAnalysisResult result =
                fraudDetectionService.analyzeTransaction(event);

            // Step 2 — act on verdict
            switch (result.getVerdict()) {

                case "LEGITIMATE" -> {
                    log.info("VERDICT: LEGITIMATE transactionId={} " +
                             "confidence={}",
                        event.getTransactionId(),
                        result.getConfidenceScore());
                    fraudEventProducer.publishApproved(
                        event, result.getConfidenceScore());
                }

                case "SUSPICIOUS" -> {
                    log.warn("VERDICT: SUSPICIOUS transactionId={} " +
                             "riskScore={} reasons={}",
                        event.getTransactionId(),
                        result.getRiskScore(),
                        result.getReasons());
                    fraudEventProducer.publishFlagged(event, result);
                }

                case "FRAUD" -> {
                    log.error("VERDICT: FRAUD DETECTED transactionId={} " +
                              "confidence={} reasons={}",
                        event.getTransactionId(),
                        result.getConfidenceScore(),
                        result.getReasons());

                    // Block the transaction
                    fraudEventProducer.publishBlocked(event, result);

                    // Freeze the account
                    fraudEventProducer.publishAccountFreeze(event, result);

                    log.error("Account {} frozen due to fraud detection",
                        event.getSourceAccountId());
                }

                default -> {
                    log.warn("Unknown verdict {} for transactionId={} " +
                             "— flagging as suspicious",
                        result.getVerdict(),
                        event.getTransactionId());
                    fraudEventProducer.publishFlagged(event, result);
                }
            }

            // Step 3 — commit offset ONLY after everything succeeded
            ack.acknowledge();

            log.info("Successfully processed transactionId={} " +
                     "verdict={} correlationId={}",
                event.getTransactionId(),
                result.getVerdict(),
                event.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to process TransactionInitiatedEvent " +
                      "transactionId={} correlationId={} " +
                      "— will retry via DLT",
                event.getTransactionId(),
                event.getCorrelationId(), e);
            // Do NOT ack — Kafka will redeliver
            // After max retries — goes to transaction-initiated.DLT
        }
    }

    /**
     * DLT handler — catches transactions that failed all retries
     * In production — alert on-call team, write to error DB
     */
    @KafkaListener(
        topics = "transaction-initiated.DLT",
        groupId = "fraud-dlt-handler"
    )
    public void handleDlt(
            @Payload String rawEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage,
            Acknowledgment ack) {

        log.error("DLT: Fraud analysis failed permanently");
        log.error("  Topic   : {}", topic);
        log.error("  Error   : {}", errorMessage);
        log.error("  Payload : {}", rawEvent);
        // TODO production: write to error_log table,
        //                  page on-call fraud team,
        //                  flag transaction as MANUAL_REVIEW

        ack.acknowledge();
    }
}