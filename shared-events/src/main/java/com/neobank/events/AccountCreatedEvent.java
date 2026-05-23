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
public class AccountCreatedEvent {

    // ── Event metadata ──────────────────────────────────
    private String eventId;           // UUID — unique ID of this event
    private String eventType;         // always "ACCOUNT_CREATED"
    private String version;           // always "v1" — for schema evolution
    private String correlationId;     // trace ID — links all events for one flow

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;        // when this event was created (UTC)

    // ── Account details ─────────────────────────────────
    private String accountId;         // UUID of the new account
    private String customerId;        // UUID of the customer
    private String accountNumber;     // e.g. "NB-2024-00123456"
    private String accountType;       // SAVINGS / CURRENT / SALARY
    private String currency;          // INR / USD / EUR

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal initialDeposit; // opening balance

    private String status;            // ACTIVE / PENDING_KYC

    // ── Customer snapshot (denormalized for consumers) ──
    // Consumers should not need to call Account Service
    // just to get the customer name for a notification
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // ── Source ──────────────────────────────────────────
    private String createdBy;         // staff ID or "SELF_SERVICE"
    private String channel;           // MOBILE / WEB / BRANCH / API
    private String ipAddress;         // for fraud audit trail
}