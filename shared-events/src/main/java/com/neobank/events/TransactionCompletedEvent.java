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
public class TransactionCompletedEvent {

    // ── Event metadata ──────────────────────────────────
    private String eventId;
    private String eventType;           // "TRANSACTION_COMPLETED"
    private String version;             // "v1"
    private String correlationId;       // same across entire transaction flow

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    // ── Transaction details ──────────────────────────────
    private String transactionId;
    private String transactionType;     // TRANSFER / DEPOSIT etc.
    private String referenceNumber;     // UTR / RRN number for reconciliation

    // ── Source account ───────────────────────────────────
    private String sourceAccountId;
    private String sourceAccountNumber;
    private String sourceCustomerId;
    private String sourceCustomerName;
    private String sourceCustomerEmail;
    private String sourceCustomerPhone;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal sourceBalanceBefore;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal sourceBalanceAfter;

    // ── Target account ───────────────────────────────────
    private String targetAccountId;
    private String targetAccountNumber;
    private String targetCustomerId;
    private String targetCustomerName;
    private String targetBankName;

    // ── Amount ───────────────────────────────────────────
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;
    private String currency;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal feeAmount;

    // ── Context (for notification service) ───────────────
    private String merchantName;
    private String category;            // AI categorized — FOOD / TRAVEL etc.
    private String description;
    private String channel;

    // ── Timing ───────────────────────────────────────────
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant initiatedAt;        // when customer hit submit

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant completedAt;        // when money actually moved

    private Long processingTimeMs;      // completedAt - initiatedAt in ms
}