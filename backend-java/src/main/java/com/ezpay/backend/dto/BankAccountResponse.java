package com.ezpay.backend.dto;

public record BankAccountResponse(
        String accountId,
        String maskedAccountNumber,
        String ifscCode,
        String accountHolderName,
        boolean isVerified
) {
}
