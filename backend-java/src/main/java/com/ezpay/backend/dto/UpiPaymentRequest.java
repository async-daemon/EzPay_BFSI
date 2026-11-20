package com.ezpay.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpiPaymentRequest(
        @NotBlank String recipientUpiId,
        @Positive double amount,
        @Size(max = 255) String note
) {
}
