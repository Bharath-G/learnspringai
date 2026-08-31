package com.neobank.account.repository;

import com.neobank.account.model.AccountByCustomer;
import com.neobank.account.model.AccountByCustomerKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountByCustomerRepository extends CassandraRepository<AccountByCustomer, AccountByCustomerKey> {

    @Query("SELECT * FROM accounts_by_customer where customer_id = ?0")
    List<AccountByCustomer> findAllByCustomerId(UUID customerId);
}
