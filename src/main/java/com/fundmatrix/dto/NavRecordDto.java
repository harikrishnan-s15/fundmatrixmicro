package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.NavStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NavRecordDto(
        Long id,
        Long schemeId,
        String schemeName,
        Long optionId,
        String optionType,
        LocalDate navDate,
        BigDecimal navValue,
        BigDecimal totalAum,
        BigDecimal totalUnitsOutstanding,
        Long publishedById,
        NavStatus status
) {
}
