package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.CutOffStatus;
import com.fundmatrix.domain.enums.TransactionStatus;
import com.fundmatrix.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionDto(
        Long id,
        String transactionRef,
        Long folioId,
        String folioNumber,
        Long schemeId,
        String schemeName,
        Long optionId,
        String optionType,
        TransactionType transactionType,
        BigDecimal amount,
        BigDecimal units,
        BigDecimal applicableNav,
        Instant transactionDate,
        CutOffStatus cutOffStatus,
        TransactionStatus status,
        BigDecimal exitLoadAmount,
        String remarks
) {
}
