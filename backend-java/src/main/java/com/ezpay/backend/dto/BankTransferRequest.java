package com.ezpay.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BankTransferRequest(
        @NotBlank String fromAccountId,
        @NotBlank String recipientAccountNumber,
        @NotBlank String recipientIfscCode,
        @Positive double amount,
        @Size(max = 255) String purpose
) {
}
