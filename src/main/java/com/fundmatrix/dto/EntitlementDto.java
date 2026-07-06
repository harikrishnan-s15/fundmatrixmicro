package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.EntitlementStatus;
import com.fundmatrix.domain.enums.PayoutMode;

import java.math.BigDecimal;

public record EntitlementDto(
        Long id,
        Long declarationId,
        Long folioId,
        String folioNumber,
        String investorName,
        BigDecimal unitsOnRecordDate,
        BigDecimal grossDividend,
        BigDecimal taxDeducted,
        BigDecimal netDividend,
        PayoutMode payoutMode,
        EntitlementStatus status
) {
}
