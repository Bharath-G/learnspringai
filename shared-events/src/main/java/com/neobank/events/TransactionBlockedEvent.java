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
public class TransactionBlockedEvent {

    // ── Event metadata ──────────────────────────────────
    private String eventId;
    private String eventType;           // "TRANSACTION_BLOCKED"
    private String version;             // "v1"
    private String correlationId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    // ── Transaction reference ────────────────────────────
    private String transactionId;
    private String transactionType;

    // ── Source account ───────────────────────────────────
    private String sourceAccountId;
    private String sourceAccountNumber;
    private String sourceCustomerId;
    private String sourceCustomerEmail;
    private String sourceCustomerPhone;
    private String sourceCustomerName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal attemptedAmount;
    private String currency;

    // ── Target (the suspected fraudulent destination) ────
    private String targetAccountNumber;
    private String targetBankName;
    private String merchantName;

    // ── Fraud verdict ────────────────────────────────────
    private String fraudVerdict;        // "FRAUD"
    private Double fraudConfidenceScore; // e.g. 0.96 = 96% sure it is fraud
    private List<String> fraudReasons;
                                        // ["amount 10x above average",
                                        //  "known fraudulent merchant",
                                        //  "3 rapid transactions in 60s",
                                        //  "new device + new location"]

    private String fraudCategory;       // PHISHING / ACCOUNT_TAKEOVER /
                                        // CARD_FRAUD / MONEY_LAUNDERING /
                                        // SOCIAL_ENGINEERING / UNKNOWN

    // ── Actions taken ────────────────────────────────────
    private Boolean accountFrozen;      // was account frozen as result?
    private Boolean refundRequired;     // was any amount already debited?
    private String refundTransactionId; // reversal ID if refund initiated

    // ── Regulatory ───────────────────────────────────────
    private Boolean reportedToRegulator; // RBI reporting for large fraud
    private String regulatoryReference;  // report reference number
    private String caseId;               // internal fraud case ID

    // ── Customer communication ───────────────────────────
    private String alertMessage;         // "We blocked a suspicious transaction
                                         //  of ₹50,000 to protect your account."
}