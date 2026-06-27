package com.neobank.fraud.repository;

import com.neobank.fraud.model.TransactionHistory;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository
    extends CassandraRepository<TransactionHistory, UUID> {

    /**
     * Fetch last 30 transactions for an account
     * Used as context for AI fraud analysis
     *
     * Cassandra returns newest first automatically
     * because of CLUSTERING ORDER BY transaction_time DESC
     */
    @Query("SELECT * FROM transactions_by_account " +
           "WHERE account_id = ?0 LIMIT 30")
    List<TransactionHistory> findLast30ByAccountId(UUID accountId);

    /**
     * Fetch transactions from last 24 hours
     * Used to detect rapid succession attacks
     */
    @Query("SELECT * FROM transactions_by_account " +
           "WHERE account_id = ?0 " +
           "AND transaction_time >= ?1 LIMIT 50")
    List<TransactionHistory> findByAccountIdSince(
        UUID accountId, java.time.Instant since);
}