package com.neobank.events;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionInitiatedEvent {
    private String transactionId;
    // Add additional fields as needed
}
