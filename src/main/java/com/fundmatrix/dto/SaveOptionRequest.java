package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.OptionStatus;
import com.fundmatrix.domain.enums.OptionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveOptionRequest(
        @NotNull OptionType optionType,
        @Size(max = 20) String isin,
        OptionStatus status
) {
}
