package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.CommissionModel;
import com.fundmatrix.domain.enums.DistributorStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Empanel or update a distributor (Admin). */
public record SaveDistributorRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 30) String arnNumber,
        @Size(max = 30) String euinNumber,
        LocalDate empanelmentDate,
        @NotNull CommissionModel commissionModel,
        DistributorStatus status,
        /** Optional login user to link to this distributor. */
        Long userId
) {
}
