package com.neobank.events;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountFrozenEvent {
    private String accountId;
    // Add additional fields as needed
}
