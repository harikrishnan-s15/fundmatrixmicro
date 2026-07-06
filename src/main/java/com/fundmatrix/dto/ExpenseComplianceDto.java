package com.fundmatrix.dto;

import java.math.BigDecimal;

/**
 * Expense-ratio compliance snapshot for a scheme: the configured TER limit vs the rate
 * currently charged (sum of the latest annualised rate per expense type).
 * status: OK | WARN (>=80% of limit) | BREACH (>limit) | NO_LIMIT.
 */
public record ExpenseComplianceDto(
        Long schemeId,
        String schemeName,
        BigDecimal expenseRatioLimit,
        BigDecimal chargedRate,
        BigDecimal utilisationPct,
        String status
) {
}
