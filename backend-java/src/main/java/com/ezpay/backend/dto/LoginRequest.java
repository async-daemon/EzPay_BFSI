package com.ezpay.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code identifier} is either the account email or mobile number. */
public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
}
