package com.fundmatrix.transaction.dto;

public record UserDto(
        Long id,
        String name,
        String email,
        String phone,
        String role,
        String status
) {
}
