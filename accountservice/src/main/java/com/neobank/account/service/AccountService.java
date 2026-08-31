package com.neobank.account.service;

import com.neobank.account.constant.Constant;
import com.neobank.account.model.*;
import com.neobank.account.repository.AccountByCustomerRepository;
import com.neobank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountByCustomerRepository accountByCustomerRepository;

    public AccountResponse createAccount(CreateAccountRequest accountRequest){
        // 1. generate ONE UUID
        UUID accountId = UUID.randomUUID();
        // 2. build Account object, save to accountRepository
        Account account = new Account();
        account.setAccountId(accountId);
        account.setAccountType(accountRequest.getAccountType());
        account.setCurrency(Constant.INR);
        account.setCustomerId(accountRequest.getCustomerId());
        account.setStatus(Constant.ACTIVE);
        account.setCreatedAt(Instant.now());
        accountRepository.save(account);
        // 3. build AccountByCustomer object (with AccountByCustomerKey), save to accountByCustomerRepository
        AccountByCustomer accountByCustomer = new AccountByCustomer();
        accountByCustomer.setAccountType(accountRequest.getAccountType());
        AccountByCustomerKey key = new AccountByCustomerKey();
        key.setAccountId(accountId);
        key.setCustomerId(accountRequest.getCustomerId());
        accountByCustomer.setKey(key);
        accountByCustomer.setStatus(Constant.ACTIVE);
        accountByCustomerRepository.save(accountByCustomer);
        // 4. return the created account
        return AccountResponse.builder()
                .accountId(accountId)
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }

    public AccountResponse getAccountById(UUID accountId){
        return accountRepository.findById(accountId)
                .map(account -> AccountResponse.builder()
                        .accountId(account.getAccountId())
                        .customerId(account.getCustomerId())
                        .accountType(account.getAccountType())
                        .balance(account.getBalance())
                        .currency(account.getCurrency())
                        .status(account.getStatus())
                        .createdAt(account.getCreatedAt())
                        .build()
                )
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found for id : " + accountId));
    }

}
