package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.AllotmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AllotmentDto(
        Long id,
        Long transactionId,
        String transactionRef,
        BigDecimal unitsAllotted,
        BigDecimal allotmentNav,
        LocalDate allotmentDate,
        AllotmentStatus status
) {
}
