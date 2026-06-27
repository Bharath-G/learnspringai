package com.neobank.fraud.producer;

import com.neobank.events.*;
import com.neobank.fraud.model.FraudAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Transaction passed fraud check — approve it
     */
    public void publishApproved(TransactionInitiatedEvent initiated,
                                 double confidenceScore) {

        TransactionApprovedEvent event = TransactionApprovedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("TRANSACTION_APPROVED")
            .version("v1")
            .correlationId(initiated.getCorrelationId())
            .timestamp(Instant.now())
            .transactionId(initiated.getTransactionId())
            .sourceAccountId(initiated.getSourceAccountId())
            .targetAccountId(initiated.getTargetAccountId())
            .amount(initiated.getAmount())
            .currency(initiated.getCurrency())
            .fraudVerdict("LEGITIMATE")
            .fraudConfidenceScore(confidenceScore)
            .approvedBy("FRAUD_DETECTION_SERVICE")
            .fraudCheckedAt(Instant.now())
            .build();

        send("transaction-approved",
            initiated.getTransactionId(), event, "TransactionApprovedEvent");
    }

    /**
     * Transaction is suspicious — hold for review
     */
    public void publishFlagged(TransactionInitiatedEvent initiated,
                                FraudAnalysisResult result) {

        TransactionFlaggedEvent event = TransactionFlaggedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("TRANSACTION_FLAGGED")
            .version("v1")
            .correlationId(initiated.getCorrelationId())
            .timestamp(Instant.now())
            .transactionId(initiated.getTransactionId())
            .sourceAccountId(initiated.getSourceAccountId())
            .sourceAccountNumber(initiated.getSourceAccountNumber())
            .sourceCustomerId(initiated.getSourceCustomerId())
            .amount(initiated.getAmount())
            .currency(initiated.getCurrency())
            .merchantName(initiated.getMerchantName())
            .fraudVerdict("SUSPICIOUS")
            .fraudConfidenceScore(result.getConfidenceScore())
            .suspiciousReasons(result.getReasons())
            .riskLevel(getRiskLevel(result.getRiskScore()))
            .transactionStatus("PENDING")
            .transactionHeld(true)
            .requiresCustomerVerification(true)
            .verificationMethod("OTP")
            .build();

        send("fraud-alerts",
            initiated.getTransactionId(), event, "TransactionFlaggedEvent");
    }

    /**
     * Transaction is fraud — block it
     */
    public void publishBlocked(TransactionInitiatedEvent initiated,
                                FraudAnalysisResult result) {

        TransactionBlockedEvent event = TransactionBlockedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("TRANSACTION_BLOCKED")
            .version("v1")
            .correlationId(initiated.getCorrelationId())
            .timestamp(Instant.now())
            .transactionId(initiated.getTransactionId())
            .transactionType(initiated.getTransactionType())
            .sourceAccountId(initiated.getSourceAccountId())
            .sourceAccountNumber(initiated.getSourceAccountNumber())
            .sourceCustomerId(initiated.getSourceCustomerId())
            .attemptedAmount(initiated.getAmount())
            .currency(initiated.getCurrency())
            .targetAccountNumber(initiated.getTargetAccountNumber())
            .targetBankName(initiated.getTargetBankName())
            .merchantName(initiated.getMerchantName())
            .fraudVerdict("FRAUD")
            .fraudConfidenceScore(result.getConfidenceScore())
            .fraudReasons(result.getReasons())
            .fraudCategory(result.getFraudCategory())
            .accountFrozen(true)
            .refundRequired(false)
            .alertMessage(
                "We blocked a suspicious transaction of ₹"
                + initiated.getAmount()
                + " to protect your account. "
                + "If this was you, please contact support.")
            .build();

        send("fraud-alerts",
            initiated.getTransactionId(), event, "TransactionBlockedEvent");
    }

    /**
     * Freeze account after fraud detected
     */
    public void publishAccountFreeze(TransactionInitiatedEvent initiated,
                                      FraudAnalysisResult result) {

        AccountFrozenEvent event = AccountFrozenEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("ACCOUNT_FROZEN")
            .version("v1")
            .correlationId(initiated.getCorrelationId())
            .timestamp(Instant.now())
            .accountId(initiated.getSourceAccountId())
            .customerId(initiated.getSourceCustomerId())
            .reason("FRAUD_DETECTED")
            .severity("HIGH")
            .frozenBy("FRAUD_DETECTION_SERVICE")
            .triggerTransactionId(initiated.getTransactionId())
            .reviewTicketId("TKT-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase())
            .requiresManualReview(true)
            .build();

        send("account-frozen",
            initiated.getSourceAccountId(), event, "AccountFrozenEvent");
    }

    /**
     * Generic send with logging and error handling
     */
    private void send(String topic, String key,
                      Object event, String eventType) {
        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} to topic={} key={}",
                        eventType, topic, key, ex);
                } else {
                    log.info("Published {} to topic={} partition={} offset={}",
                        eventType, topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }

    private String getRiskLevel(Integer riskScore) {
        if (riskScore == null) return "MEDIUM";
        if (riskScore <= 3) return "LOW";
        if (riskScore <= 6) return "MEDIUM";
        if (riskScore <= 8) return "HIGH";
        return "CRITICAL";
    }
}