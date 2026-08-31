package com.neobank.account.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private UUID accountId;
    private UUID customerId;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
    private Instant createdAt;

}
