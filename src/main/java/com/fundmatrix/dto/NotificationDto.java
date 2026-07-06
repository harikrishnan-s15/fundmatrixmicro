package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.NotificationCategory;
import com.fundmatrix.domain.enums.NotificationStatus;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String message,
        NotificationCategory category,
        NotificationStatus status,
        Instant createdDate
) {
}
