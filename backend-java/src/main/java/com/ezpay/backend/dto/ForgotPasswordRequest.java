package com.ezpay.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank String identifier) {
}
