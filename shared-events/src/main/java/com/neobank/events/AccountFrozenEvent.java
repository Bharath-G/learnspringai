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
public class AccountFrozenEvent {

    // ── Event metadata ──────────────────────────────────
    private String eventId;
    private String eventType;         // "ACCOUNT_FROZEN"
    private String version;           // "v1"
    private String correlationId;     // same as the transaction that triggered freeze

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    // ── Account details ─────────────────────────────────
    private String accountId;
    private String customerId;
    private String accountNumber;

    // ── Freeze details ───────────────────────────────────
    private String reason;            // FRAUD_DETECTED / KYC_EXPIRED /
                                      // SUSPICIOUS_ACTIVITY / MANUAL_REVIEW /
                                      // COURT_ORDER / CUSTOMER_REQUEST

    private String severity;          // LOW / MEDIUM / HIGH / CRITICAL

    private String frozenBy;          // "FRAUD_DETECTION_SERVICE" / staff ID

    private String triggerTransactionId; // which transaction caused the freeze

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal balanceAtFreeze;  // snapshot of balance when frozen

    // ── Notification targets ─────────────────────────────
    private String customerEmail;
    private String customerPhone;

    // ── Resolution ───────────────────────────────────────
    private String reviewTicketId;    // support ticket created for review
    private Boolean requiresManualReview; // true = needs human review
}