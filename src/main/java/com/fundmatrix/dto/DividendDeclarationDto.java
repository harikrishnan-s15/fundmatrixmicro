package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.DividendStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DividendDeclarationDto(
        Long id,
        Long schemeId,
        String schemeName,
        Long optionId,
        String optionType,
        LocalDate recordDate,
        BigDecimal dividendPerUnit,
        BigDecimal totalDistributionAmount,
        Long declaredById,
        DividendStatus status,
        long entitlementCount
) {
}
