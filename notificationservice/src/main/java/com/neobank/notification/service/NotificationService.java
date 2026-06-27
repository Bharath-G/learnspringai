package com.neobank.notification.service;

import com.neobank.events.*;
import com.neobank.notification.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void notifyAccountCreated(AccountCreatedEvent event) {
        // Replace with real email/SMS later
        log.info("NOTIFICATION [ACCOUNT CREATED]");
        log.info("  To      : {} <{}>",
                event.getCustomerName(), event.getCustomerEmail());
        log.info("  Account : {}", event.getAccountNumber());
        log.info("  Type    : {}", event.getAccountType());
        log.info("  Message : Welcome to NeoBank! " +
                        "Your {} account {} is now active.",
                event.getAccountType(), event.getAccountNumber());
    }

    public void notifyTransactionCompleted(TransactionCompletedEvent event) {
        log.info("NOTIFICATION [TRANSACTION COMPLETED]");
        log.info("  To        : {} <{}>",
                event.getSourceCustomerName(), event.getSourceCustomerEmail());
        log.info("  TxnId     : {}", event.getTransactionId());
        log.info("  Amount    : {} {}", event.getCurrency(), event.getAmount());
        log.info("  To Acc    : {}", event.getTargetAccountNumber());
        log.info("  Balance   : {} after transaction",
                event.getSourceBalanceAfter());
        log.info("  Message   : ₹{} debited. Available balance: ₹{}",
                event.getAmount(), event.getSourceBalanceAfter());
    }

    public void notifyTransactionFailed(TransactionFailedEvent event) {
        log.info("NOTIFICATION [TRANSACTION FAILED]");
        log.info(Constants.TO, event.getSourceCustomerEmail());
        log.info("  TxnId   : {}", event.getTransactionId());
        log.info(Constants.REASON, event.getFailureReason());
        log.info("  Retry   : {}", event.getRetryable());
        log.info("  Message : Transaction failed — {}",
                event.getFailureReason());
    }

    public void notifyTransactionFlagged(TransactionFlaggedEvent event) {
        log.info("NOTIFICATION [TRANSACTION FLAGGED]");
        log.info(Constants.TO, event.getSourceCustomerEmail());
        log.info("  TxnId   : {}", event.getTransactionId());
        log.info("  Risk    : {}", event.getRiskLevel());
        log.info("  Reasons : {}", event.getSuspiciousReasons());
        log.info("  Message : Suspicious activity detected on your account. " +
                "Transaction held for review.");
    }

    public void notifyAccountFrozen(AccountFrozenEvent event) {
        log.info("NOTIFICATION [ACCOUNT FROZEN]");
        log.info(Constants.TO, event.getCustomerEmail());
        log.info("  Account : {}", event.getAccountNumber());
        log.info(Constants.REASON, event.getReason());
        log.info("  Ticket  : {}", event.getReviewTicketId());
        log.info("  Message : Your account {} has been frozen. " +
                        "Reference: {}. Contact support.",
                event.getAccountNumber(), event.getReviewTicketId());
    }

    public void notifyTransactionBlocked(TransactionBlockedEvent event) {
        log.info("NOTIFICATION [TRANSACTION BLOCKED]");
        log.info(Constants.TO, event.getSourceCustomerEmail());
        log.info("  Amount  : {} {}", event.getCurrency(),
                event.getAttemptedAmount());
        log.info(Constants.REASON, event.getFraudCategory());
        log.info("  Message : {}", event.getAlertMessage());
    }
}