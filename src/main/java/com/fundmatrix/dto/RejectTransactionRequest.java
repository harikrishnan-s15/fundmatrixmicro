package com.fundmatrix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectTransactionRequest(
        @NotBlank @Size(max = 255) String reason
) {
}
