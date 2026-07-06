package com.fundmatrix.transaction.dto;

import com.fundmatrix.transaction.domain.TransactionStatus;
import com.fundmatrix.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
        Long id,
        Long folioId,
        Long schemeId,
        Long userId,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal units,
        BigDecimal nav,
        LocalDateTime transactionDate,
        String remarks
) {
}
