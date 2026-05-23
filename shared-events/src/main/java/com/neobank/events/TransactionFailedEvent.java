package com.neobank.events;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFailedEvent {
    private String transactionId;
    private String reason;
    // Add additional fields as needed
}
