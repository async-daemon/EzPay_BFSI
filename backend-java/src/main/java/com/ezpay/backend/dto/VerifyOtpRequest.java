package com.ezpay.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(@NotBlank String mobileNumber, @NotBlank String otp) {
}
