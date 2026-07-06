package com.fundmatrix.transaction.dto;

import com.fundmatrix.transaction.domain.TransactionType;

import java.math.BigDecimal;

public record SubscriptionRequest(
        Long folioId,
        Long schemeId,
        BigDecimal amount,
        BigDecimal nav
) {
}
