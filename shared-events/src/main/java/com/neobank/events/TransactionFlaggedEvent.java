package com.neobank.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFlaggedEvent {

    // ── Event metadata ──────────────────────────────────
    private String eventId;
    private String eventType;           // "TRANSACTION_FLAGGED"
    private String version;             // "v1"
    private String correlationId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    // ── Transaction reference ────────────────────────────
    private String transactionId;
    private String sourceAccountId;
    private String sourceAccountNumber;
    private String sourceCustomerId;
    private String sourceCustomerEmail;
    private String sourceCustomerPhone;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;
    private String currency;
    private String merchantName;

    // ── Fraud analysis ───────────────────────────────────
    private String fraudVerdict;        // "SUSPICIOUS"
    private Double fraudConfidenceScore; // e.g. 0.65 = borderline suspicious
    private List<String> suspiciousReasons;
                                        // ["amount 3x above average",
                                        //  "new beneficiary",
                                        //  "unusual time 2:47 AM"]

    private String riskLevel;           // LOW / MEDIUM / HIGH

    // ── Current status ───────────────────────────────────
    private String transactionStatus;   // "PENDING" — held for review
    private Boolean transactionHeld;    // true = money not moved yet

    // ── Review details ───────────────────────────────────
    private String reviewTicketId;      // support ticket for human review
    private String assignedTo;          // fraud team queue / specific analyst
    private String reviewDeadline;      // SLA — "must review within 2 hours"

    // ── Customer action required ─────────────────────────
    private Boolean requiresCustomerVerification; // send OTP / call bank?
    private String verificationMethod;  // "OTP" / "CALL_BANK" / "VISIT_BRANCH"
}