package com.fundmatrix.transaction.dto;

public record FolioDto(
        Long id,
        Long userId,
        Long schemeId,
        String status,
        java.math.BigDecimal currentValue
) {
}
