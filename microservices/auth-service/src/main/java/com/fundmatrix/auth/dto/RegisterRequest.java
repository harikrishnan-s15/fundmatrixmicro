package com.fundmatrix.auth.dto;

public record RegisterRequest(String name, String email, String phone, String password) {
}
