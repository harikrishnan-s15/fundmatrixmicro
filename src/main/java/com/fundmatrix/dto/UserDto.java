package com.fundmatrix.dto;

import com.fundmatrix.domain.enums.Role;
import com.fundmatrix.domain.enums.UserStatus;

import java.time.Instant;

public record UserDto(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        UserStatus status,
        Instant createdAt
) {
}
