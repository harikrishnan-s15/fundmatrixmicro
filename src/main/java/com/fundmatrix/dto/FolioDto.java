package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.FolioStatus;
import com.fundmatrix.domain.enums.ModeOfHolding;
import com.fundmatrix.domain.enums.TaxStatus;

import java.math.BigDecimal;

public record FolioDto(
        Long id,
        String folioNumber,
        Long investorId,
        String investorName,
        Long distributorId,
        String distributorName,
        TaxStatus taxStatus,
        ModeOfHolding modeOfHolding,
        String nomineeDetails,
        String bankAccountRef,
        FolioStatus status,
        BigDecimal currentValue
) {
}
