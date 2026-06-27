package com.neobank.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobank.events.TransactionInitiatedEvent;
import com.neobank.fraud.constants.Constant;
import com.neobank.fraud.model.FraudAnalysisResult;
import com.neobank.fraud.model.TransactionHistory;
import com.neobank.fraud.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final ChatClient chatClient;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Main fraud analysis method
     * Called by consumer for every transaction initiated
     */
    public FraudAnalysisResult analyzeTransaction(
            TransactionInitiatedEvent event) {

        log.info("Starting fraud analysis for transactionId={} amount={} " +
                 "correlationId={}",
            event.getTransactionId(),
            event.getAmount(),
            event.getCorrelationId());

        try {
            // Step 1 — fetch transaction history from Cassandra
            List<TransactionHistory> history = fetchTransactionHistory(
                event.getSourceAccountId());

            log.debug("Fetched {} historical transactions for accountId={}",
                history.size(), event.getSourceAccountId());

            // Step 2 — calculate stats for better AI context
            TransactionStats stats = calculateStats(history, event.getAmount());

            // Step 3 — build prompt
            String prompt = buildFraudAnalysisPrompt(event, history, stats);

            log.debug("Sending prompt to Ollama mistral model");

            // Step 4 — call Spring AI — this is the magic line
            FraudAnalysisResult result = chatClient
                .prompt()
                .user(prompt)
                .call()
                .entity(FraudAnalysisResult.class);

            // Step 5 — validate result (AI can sometimes return null fields)
            result = validateAndSanitizeResult(result, event);

            log.info("Fraud analysis complete transactionId={} " +
                     "verdict={} confidence={} riskScore={}",
                event.getTransactionId(),
                result.getVerdict(),
                result.getConfidenceScore(),
                result.getRiskScore());

            return result;

        } catch (Exception e) {
            log.error("Fraud analysis failed for transactionId={} — " +
                      "defaulting to SUSPICIOUS for safety",
                event.getTransactionId(), e);

            // If AI fails — mark as suspicious, not approve
            // Never approve by default on AI failure — safety first
            return buildFallbackResult(event, e.getMessage());
        }
    }

    /**
     * Fetch transaction history from Cassandra
     */
    private List<TransactionHistory> fetchTransactionHistory(
            String accountId) {
        try {
            return transactionRepository
                .findLast30ByAccountId(UUID.fromString(accountId));
        } catch (Exception e) {
            log.warn("Could not fetch transaction history " +
                     "for accountId={} — proceeding with empty history",
                accountId, e);
            return List.of();
        }
    }

    /**
     * Calculate statistical context for AI
     * Helps AI understand what is "normal" for this account
     */
    private TransactionStats calculateStats(
            List<TransactionHistory> history,
            BigDecimal currentAmount) {

        if (history.isEmpty()) {
            return new TransactionStats(
                BigDecimal.ZERO, BigDecimal.ZERO, 0, false);
        }

        // Average transaction amount over last 30
        BigDecimal avg = history.stream()
            .map(TransactionHistory::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        // Max single transaction
        BigDecimal max = history.stream()
            .map(TransactionHistory::getAmount)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        // Count transactions in last 1 hour
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long txnsLastHour = history.stream()
            .filter(t -> t.getTransactionTime().isAfter(oneHourAgo))
            .count();

        // Is current amount more than 3x the average?
        boolean isUnusuallyLarge = avg.compareTo(BigDecimal.ZERO) > 0
            && currentAmount.compareTo(avg.multiply(BigDecimal.valueOf(3))) > 0;

        return new TransactionStats(avg, max, txnsLastHour, isUnusuallyLarge);
    }

    /**
     * Build the fraud analysis prompt
     * This is the most important method — prompt quality = AI quality
     */
    private String buildFraudAnalysisPrompt(
            TransactionInitiatedEvent event,
            List<TransactionHistory> history,
            TransactionStats stats) {

        // Format transaction history for AI
        StringBuilder historyText = new StringBuilder();
        if (history.isEmpty()) {
            historyText.append("No previous transactions found " +
                               "(new account or first transaction)");
        } else {
            history.forEach(t -> historyText
                .append(String.format("  %s | %-6s | %10s | %-20s | %s%n",
                    t.getTransactionTime(),
                    t.getType(),
                    t.getAmount(),
                    t.getMerchant() != null ? t.getMerchant() : "N/A",
                    t.getStatus())));
        }

        return """
            You are a banking fraud detection AI for NeoBank India.
            Analyse this transaction and return ONLY a JSON response.
            
            ═══ TRANSACTION UNDER REVIEW ═══
            Transaction ID  : %s
            Type            : %s
            Amount          : ₹%s %s
            From Account    : %s
            To Account      : %s
            To Bank         : %s
            Merchant        : %s
            Channel         : %s
            Device ID       : %s
            IP Address      : %s
            Time            : %s
            
            ═══ ACCOUNT STATISTICS (last 30 transactions) ═══
            Average transaction amount : ₹%s
            Largest transaction ever   : ₹%s
            Transactions in last 1 hour: %d
            Current amount vs average  : %s
            
            ═══ TRANSACTION HISTORY (newest first) ═══
            %s
            
            ═══ FRAUD INDICATORS TO CHECK ═══
            1. Is the amount unusually large compared to history?
            2. Is this a new beneficiary never transacted with before?
            3. Are there multiple rapid transactions (velocity attack)?
            4. Is the transaction time unusual (late night 11PM-5AM)?
            5. Is the merchant or category completely new for this customer?
            6. Is the IP address or device different from usual?
            7. Does this match known fraud patterns (round amounts, test transactions)?
            
            ═══ RESPOND IN THIS EXACT JSON FORMAT ═══
            {
              "verdict": "LEGITIMATE" or "SUSPICIOUS" or "FRAUD",
              "confidenceScore": (number between 0.0 and 1.0),
              "riskScore": (integer 1-10, 1=very safe, 10=definite fraud),
              "recommendedAction": "APPROVE" or "FLAG_FOR_REVIEW" or "BLOCK",
              "fraudCategory": "NONE" or "PHISHING" or "ACCOUNT_TAKEOVER" or "CARD_FRAUD" or "MONEY_LAUNDERING" or "SOCIAL_ENGINEERING" or "UNKNOWN",
              "reasons": ["reason1", "reason2"]
            }
            
            RULES:
            - verdict LEGITIMATE → recommendedAction must be APPROVE
            - verdict SUSPICIOUS → recommendedAction must be FLAG_FOR_REVIEW
            - verdict FRAUD → recommendedAction must be BLOCK
            - If no history exists, be more cautious but not automatic FRAUD
            - Respond ONLY with the JSON object, no other text
            """.formatted(
                event.getTransactionId(),
                event.getTransactionType(),
                event.getAmount(), event.getCurrency(),
                event.getSourceAccountNumber(),
                event.getTargetAccountNumber(),
                event.getTargetBankName() != null
                    ? event.getTargetBankName() : "NeoBank",
                event.getMerchantName() != null
                    ? event.getMerchantName() : "N/A",
                event.getChannel() != null
                    ? event.getChannel() : Constant.UNKNOWN,
                event.getDeviceId() != null
                    ? event.getDeviceId() : Constant.UNKNOWN,
                event.getIpAddress() != null
                    ? event.getIpAddress() : Constant.UNKNOWN,
                event.getTimestamp(),
                stats.averageAmount(),
                stats.maxAmount(),
                stats.txnsLastHour(),
                stats.isUnusuallyLarge()
                    ? "⚠ UNUSUALLY LARGE (>3x average)"
                    : "Normal range"
                    ,
                historyText
            );
    }

    /**
     * Validate AI response — AI can sometimes miss fields
     * or return unexpected values
     */
    private FraudAnalysisResult validateAndSanitizeResult(
            FraudAnalysisResult result,
            TransactionInitiatedEvent event) {

        // If verdict is null — default to SUSPICIOUS
        if (result.getVerdict() == null) {
            log.warn("AI returned null verdict for transactionId={} " +
                     "— defaulting to SUSPICIOUS",
                event.getTransactionId());
            result.setVerdict(Constant.SUSPICIOUS);
            result.setRecommendedAction(Constant.FLAG_FOR_REVIEW);
        }

        // Ensure verdict and action are consistent
        switch (result.getVerdict()) {
            case "LEGITIMATE" ->
                result.setRecommendedAction("APPROVE");
            case "FRAUD" ->
                result.setRecommendedAction("BLOCK");
            case Constant.SUSPICIOUS ->
                result.setRecommendedAction(Constant.FLAG_FOR_REVIEW);
            default -> {
                log.warn("Unknown verdict {} — defaulting to SUSPICIOUS",
                    result.getVerdict());
                result.setVerdict(Constant.SUSPICIOUS);
                result.setRecommendedAction(Constant.FLAG_FOR_REVIEW);
            }
        }

        // Ensure confidence score is in valid range
        if (result.getConfidenceScore() == null
                || result.getConfidenceScore() < 0
                || result.getConfidenceScore() > 1) {
            result.setConfidenceScore(0.5);
        }

        // Ensure risk score is in valid range
        if (result.getRiskScore() == null
                || result.getRiskScore() < 1
                || result.getRiskScore() > 10) {
            result.setRiskScore(5);
        }

        return result;
    }

    /**
     * Fallback result when AI call fails
     * Always SUSPICIOUS — never approve on failure
     */
    private FraudAnalysisResult buildFallbackResult(
            TransactionInitiatedEvent event, String errorMessage) {

        return FraudAnalysisResult.builder()
            .verdict(Constant.SUSPICIOUS)
            .confidenceScore(0.5)
            .riskScore(5)
            .recommendedAction(Constant.FLAG_FOR_REVIEW)
            .fraudCategory(Constant.UNKNOWN)
            .reasons(List.of(
                "AI analysis failed — flagged for manual review",
                "Error: " + errorMessage))
            .build();
    }

    /**
     * Simple record to hold calculated stats
     */
    record TransactionStats(
        BigDecimal averageAmount,
        BigDecimal maxAmount,
        long txnsLastHour,
        boolean isUnusuallyLarge
    ) {}
}