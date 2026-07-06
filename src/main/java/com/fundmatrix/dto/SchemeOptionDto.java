package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.OptionStatus;
import com.fundmatrix.domain.enums.OptionType;

import java.math.BigDecimal;

public record SchemeOptionDto(
        Long id,
        Long schemeId,
        String schemeName,
        OptionType optionType,
        String isin,
        OptionStatus status,
        BigDecimal latestNav
) {
}
