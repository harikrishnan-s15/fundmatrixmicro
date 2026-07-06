package com.fundmatrix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reversing an accrual requires a mandatory reason (captured in the audit trail). */
public record ReverseAccrualRequest(
        @NotBlank @Size(max = 255) String reason
) {
}
