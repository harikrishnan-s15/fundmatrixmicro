package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.SipStatus;
import jakarta.validation.constraints.NotNull;

/** Pause / Resume (ACTIVE) / Cancel an SWP mandate. */
public record UpdateSwpStatusRequest(
        @NotNull SipStatus status
) {
}
