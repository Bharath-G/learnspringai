package com.neobank.account.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("accounts_by_id")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @PrimaryKeyColumn(
            name = "account_id",
            type = PrimaryKeyType.PARTITIONED   // partition key — filter by this
    )
    private UUID accountId;

    @Column("customer_id")
    private UUID customerId;

    @Column("account_type")
    private String accountType;

    @Column("balance")
    private BigDecimal balance;

    @Column("currency")
    private String currency;

    @Column("status")
    private String status;

    @Column("created_at")
    private Instant createdAt;
}