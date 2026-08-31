package com.neobank.account.repository;

import com.neobank.account.model.Account;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountRepository extends CassandraRepository<Account, UUID> {

}
