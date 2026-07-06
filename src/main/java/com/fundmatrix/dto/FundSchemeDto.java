package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.RiskProfile;
import com.fundmatrix.domain.enums.SchemeCategory;
import com.fundmatrix.domain.enums.SchemeStatus;

import java.math.BigDecimal;
import java.util.List;

public record FundSchemeDto(
        Long id,
        String schemeName,
        String schemeCode,
        SchemeCategory category,
        RiskProfile riskProfile,
        String benchmarkIndex,
        Long fundManagerId,
        String fundManagerName,
        BigDecimal minInvestment,
        String exitLoadSlab,
        BigDecimal exitLoadRate,
        Integer exitLoadPeriodDays,
        BigDecimal expenseRatio,
        BigDecimal minSipAmount,
        BigDecimal minSwpAmount,
        String cutoffTime,
        SchemeStatus status,
        List<SchemeOptionDto> options
) {
}
