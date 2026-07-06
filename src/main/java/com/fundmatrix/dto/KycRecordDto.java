package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.KycStatus;
import com.fundmatrix.domain.enums.KycType;

import java.time.LocalDate;

public record KycRecordDto(
        Long id,
        Long investorId,
        String investorName,
        KycType kycType,
        String documentType,
        String documentRef,
        LocalDate verifiedDate,
        KycStatus kycStatus
) {
}
