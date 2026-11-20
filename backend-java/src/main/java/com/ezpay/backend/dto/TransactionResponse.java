package com.ezpay.backend.dto;

import java.time.LocalDateTime;

public record TransactionResponse(
        String transactionId,
        String type,
        double amount,
        String counterparty,
        String note,
        LocalDateTime date,
        String status,
        String failureReason
) {
}
