package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.SchemeCategory;

import java.math.BigDecimal;

/** Per-scheme AUM line used by the Fund Accountant AUM tracker and admin analytics. */
public record AumSummaryDto(
        Long schemeId,
        String schemeName,
        String schemeCode,
        SchemeCategory category,
        BigDecimal latestNav,
        BigDecimal totalAum,
        BigDecimal totalUnitsOutstanding
) {
}
