package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.FolioStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateFolioStatusRequest(@NotNull FolioStatus status) {
}
