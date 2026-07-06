package com.fundmatrix.user.dto;

public record CreateUserRequest(
        String name,
        String email,
        String phone,
        com.fundmatrix.user.domain.Role role,
        String address,
        String city,
        String state,
        String zipcode
) {
}
