package com.ezpay.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddBankAccountRequest(
        @NotBlank @Size(min = 6, max = 34) String accountNumber,
        @NotBlank @Size(min = 11, max = 11) String ifscCode,
        @NotBlank String accountHolderName
) {
}
