package com.neobank.account.controller;

import com.neobank.account.model.AccountResponse;
import com.neobank.account.model.CreateAccountRequest;
import com.neobank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // your createAccount endpoint here
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @RequestBody CreateAccountRequest accountRequest) {
        // remember: ResponseEntity<AccountResponse>, status 201
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(accountService.createAccount(accountRequest));
    }

    @GetMapping("/{accountId}")
    public AccountResponse getAccountById(@PathVariable UUID accountId) {
        return accountService.getAccountById(accountId);
    }

}
