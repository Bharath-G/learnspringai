package com.neobank.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFailedEvent {

    // ── Event metadata ──────────────────────────────────
    private String eventId;
    private String eventType;           // "TRANSACTION_FAILED"
    private String version;             // "v1"
    private String correlationId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    // ── Transaction reference ────────────────────────────
    private String transactionId;
    private String transactionType;

    // ── Account details ──────────────────────────────────
    private String sourceAccountId;
    private String sourceAccountNumber;
    private String sourceCustomerId;
    private String sourceCustomerEmail;
    private String sourceCustomerPhone;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal attemptedAmount;
    private String currency;

    // ── Failure details ──────────────────────────────────
    private String failureCode;         // INSUFFICIENT_FUNDS / FRAUD_BLOCKED /
                                        // ACCOUNT_FROZEN / DAILY_LIMIT_EXCEEDED /
                                        // INVALID_BENEFICIARY / BANK_REJECTED /
                                        // TECHNICAL_ERROR / TIMEOUT

    private String failureReason;       // human readable — shown in notification
                                        // "Insufficient balance. Available: ₹2,000"

    private String failedBy;            // "FRAUD_DETECTION_SERVICE" /
                                        // "TRANSACTION_SERVICE" /
                                        // "PAYMENT_GATEWAY"

    // ── Fraud context (if failed due to fraud) ───────────
    private String fraudVerdict;        // "FRAUD" / null
    private Double fraudConfidenceScore;
    private java.util.List<String> fraudReasons; // ["unusual amount", "new beneficiary"]

    // ── Retry information ────────────────────────────────
    private Boolean retryable;          // can customer try again?
    private String retryAfter;          // "24 hours" / "contact support"

    // ── Refund details ───────────────────────────────────
    private Boolean refundInitiated;    // if amount was debited before failure
    private String refundTransactionId; // reversal transaction ID
}