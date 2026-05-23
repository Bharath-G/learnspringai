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
public class TransactionInitiatedEvent {

    // ── Event metadata ──────────────────────────────────
    private String eventId;
    private String eventType;         // "TRANSACTION_INITIATED"
    private String version;           // "v1"
    private String correlationId;     // this IS the transactionId for the whole flow

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    // ── Transaction core ─────────────────────────────────
    private String transactionId;     // UUID — primary key for this transaction
    private String transactionType;   // TRANSFER / DEPOSIT / WITHDRAWAL /
                                      // BILL_PAYMENT / UPI / NEFT / RTGS / IMPS

    // ── Source account ───────────────────────────────────
    private String sourceAccountId;
    private String sourceAccountNumber;
    private String sourceCustomerId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal sourceBalanceBefore; // balance before this transaction

    // ── Destination ──────────────────────────────────────
    private String targetAccountId;       // null for external transfers
    private String targetAccountNumber;
    private String targetCustomerId;      // null for external transfers
    private String targetBankCode;        // IFSC code for external transfers
    private String targetBankName;        // e.g. "HDFC Bank"

    // ── Amount ───────────────────────────────────────────
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;
    private String currency;             // INR

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal feeAmount;        // transaction fee charged

    // ── Context for fraud detection ──────────────────────
    private String merchantName;         // for card transactions
    private String merchantCategory;     // MCC code — e.g. "5411" = grocery
    private String deviceId;             // which device initiated
    private String ipAddress;
    private String location;             // city/country if available
    private String channel;              // MOBILE / WEB / ATM / BRANCH / UPI

    // ── Reference ────────────────────────────────────────
    private String description;          // "Rent payment" / "Invoice #123"
    private String referenceNumber;      // external reference / UTR number
}