package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.ExpenseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Book a fund expense accrual. When {@code accrualAmount} is omitted it is auto-computed
 * from the scheme AUM and the annualised rate for a single day.
 */
public record CreateAccrualRequest(
        @NotNull Long schemeId,
        @NotNull ExpenseType expenseType,
        @NotNull @PositiveOrZero BigDecimal annualisedRate,
        BigDecimal accrualAmount,
        LocalDate accrualDate
) {
}
