package com.neobank.fraud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Spring AI maps the Ollama JSON response directly
 * into this class using .entity(FraudAnalysisResult.class)
 *
 * The field names here MUST match the JSON keys
 * in your prompt's expected response format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // ignore extra AI fields
public class FraudAnalysisResult {

    /**
     * AI verdict on the transaction
     * Values: LEGITIMATE / SUSPICIOUS / FRAUD
     */
    private String verdict;

    /**
     * How confident the AI is — 0.0 to 1.0
     * 0.95 = 95% sure this is fraud
     * 0.45 = borderline — mark as suspicious
     */
    private Double confidenceScore;

    /**
     * Specific reasons AI flagged this transaction
     * e.g. ["amount 5x above 30-day average",
     *        "new beneficiary never transacted before",
     *        "transaction at 3:47 AM unusual for this customer"]
     */
    private List<String> reasons;

    /**
     * What action fraud service should take
     * Values: APPROVE / FLAG_FOR_REVIEW / BLOCK
     */
    private String recommendedAction;

    /**
     * Category if fraud detected
     * Values: PHISHING / ACCOUNT_TAKEOVER / CARD_FRAUD /
     *         MONEY_LAUNDERING / SOCIAL_ENGINEERING / UNKNOWN
     */
    private String fraudCategory;

    /**
     * Risk score 1-10
     * 1-3 = low, 4-6 = medium, 7-9 = high, 10 = critical
     */
    private Integer riskScore;
}