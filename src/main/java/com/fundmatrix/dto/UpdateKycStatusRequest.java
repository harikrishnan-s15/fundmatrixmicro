package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.KycStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateKycStatusRequest(@NotNull KycStatus kycStatus) {
}
