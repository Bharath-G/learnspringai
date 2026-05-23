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
public class TransactionApprovedEvent {

    // ── Event metadata ──────────────────────────────────
    private String eventId;
    private String eventType;          // "TRANSACTION_APPROVED"
    private String version;            // "v1"
    private String correlationId;      // same correlationId as initiated event

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    // ── Transaction reference ────────────────────────────
    private String transactionId;      // links back to initiated event
    private String sourceAccountId;
    private String targetAccountId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;
    private String currency;

    // ── Fraud analysis result ────────────────────────────
    private String fraudVerdict;       // "LEGITIMATE"
    private Double fraudConfidenceScore; // 0.0 to 1.0 (e.g. 0.97 = 97% sure)
    private String approvedBy;         // "FRAUD_DETECTION_SERVICE"

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant fraudCheckedAt;    // when AI completed the check

    // ── Processing instructions ──────────────────────────
    private String processingPriority; // NORMAL / HIGH (for RTGS/NEFT cutoffs)
    private Boolean requiresOtp;       // true if OTP still pending
}