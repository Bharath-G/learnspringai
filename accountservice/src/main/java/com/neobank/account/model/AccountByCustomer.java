package com.neobank.account.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;

@Table("accounts_by_customer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountByCustomer {

    @PrimaryKey
    private AccountByCustomerKey key;

    @Column("account_type")
    private String accountType;

    @Column("balance")
    private BigDecimal balance;

    @Column("status")
    private String status;

}