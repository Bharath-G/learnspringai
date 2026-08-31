package com.neobank.account.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.UUID;

@PrimaryKeyClass
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountByCustomerKey implements Serializable {

    @PrimaryKeyColumn(
            name = "customer_id",
            type = PrimaryKeyType.PARTITIONED,
            ordinal = 0
    )
    private UUID customerId;

    @PrimaryKeyColumn(
            name = "account_id",
            type = PrimaryKeyType.CLUSTERED,
            ordinal = 1
    )
    private UUID accountId;
}
