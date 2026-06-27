package com.neobank.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps to transactions_by_account table in Cassandra
 * Fraud service reads this to get transaction history
 * for AI context — never writes to this table
 */
@Table("transactions_by_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistory {

    @PrimaryKeyColumn(
        name = "account_id",
        type = PrimaryKeyType.PARTITIONED   // partition key — filter by this
    )
    private UUID accountId;

    @PrimaryKeyColumn(
        name = "transaction_time",
        type = PrimaryKeyType.CLUSTERED,
        ordering = Ordering.DESCENDING      // newest first
    )
    private Instant transactionTime;

    @PrimaryKeyColumn(
        name = "transaction_id",
        type = PrimaryKeyType.CLUSTERED
    )
    private UUID transactionId;

    @Column("type")
    private String type;                    // DEBIT / CREDIT

    @Column("amount")
    @CassandraType(type = CassandraType.Name.DECIMAL)
    private BigDecimal amount;

    @Column("balance_after")
    @CassandraType(type = CassandraType.Name.DECIMAL)
    private BigDecimal balanceAfter;

    @Column("merchant")
    private String merchant;

    @Column("category")
    private String category;

    @Column("status")
    private String status;
}