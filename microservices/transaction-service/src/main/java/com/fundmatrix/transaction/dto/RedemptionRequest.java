package com.fundmatrix.transaction.dto;

import java.math.BigDecimal;

public record RedemptionRequest(
        Long folioId,
        Long schemeId,
        BigDecimal units,
        BigDecimal nav
) {
}
