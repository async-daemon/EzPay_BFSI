package com.ezpay.backend.dto;

public record TokenResponse(String accessToken, String tokenType, String userId, String username) {
    public TokenResponse(String accessToken, String userId, String username) {
        this(accessToken, "bearer", userId, username);
    }
}
