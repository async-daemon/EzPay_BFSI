package com.ezpay.backend.dto;

public record ForgotPasswordResponse(String message, String devResetToken) {
}
