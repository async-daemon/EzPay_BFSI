package com.ezpay.backend.dto;

/** Response to {@code POST /auth/register}. */
public record RegisterResponse(String message, String mobileNumber, String devOtp) {
}
