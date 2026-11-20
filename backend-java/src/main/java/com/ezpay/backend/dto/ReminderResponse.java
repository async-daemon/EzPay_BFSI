package com.ezpay.backend.dto;

import java.time.LocalDateTime;

public record ReminderResponse(
        String reminderId,
        String title,
        double amount,
        LocalDateTime dueDate,
        String status
) {
}
