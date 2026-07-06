package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.CommissionModel;
import com.fundmatrix.domain.enums.DistributorStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DistributorDto(
        Long id,
        String name,
        String arnNumber,
        String euinNumber,
        LocalDate empanelmentDate,
        CommissionModel commissionModel,
        DistributorStatus status,
        Long userId,
        BigDecimal aumManaged
) {
}
