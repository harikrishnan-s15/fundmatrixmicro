package com.fundmatrix.user.dto;

import com.fundmatrix.user.domain.Role;
import com.fundmatrix.user.domain.UserStatus;

public record UserDto(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        UserStatus status,
        String address,
        String city,
        String state,
        String zipcode
) {
}
