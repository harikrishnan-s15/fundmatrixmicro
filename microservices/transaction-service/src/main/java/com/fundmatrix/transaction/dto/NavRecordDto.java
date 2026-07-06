package com.fundmatrix.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NavRecordDto(
        Long id,
        Long schemeId,
        BigDecimal nav,
        LocalDate navDate
) {
}
