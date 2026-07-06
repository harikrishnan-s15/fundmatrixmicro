package com.fundmatrix.auth.dto;

import com.fundmatrix.auth.domain.Role;
import com.fundmatrix.auth.domain.UserStatus;
import lombok.Builder;

public record UserDto(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        UserStatus status
) {
}
