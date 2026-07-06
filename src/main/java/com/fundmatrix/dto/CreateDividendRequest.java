package com.fundmatrix.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Declare a dividend at scheme-option level (Fund Accountant). */
public record CreateDividendRequest(
        @NotNull Long optionId,
        @NotNull LocalDate recordDate,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal dividendPerUnit
) {
}
