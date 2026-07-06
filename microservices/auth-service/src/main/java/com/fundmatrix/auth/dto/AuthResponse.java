package com.fundmatrix.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        UserDto user
) {
    public static AuthResponse bearer(String token, long expiresIn, UserDto user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
