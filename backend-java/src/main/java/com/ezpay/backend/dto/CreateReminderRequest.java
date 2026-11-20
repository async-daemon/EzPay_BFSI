package com.ezpay.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateReminderRequest(
        @NotBlank @Size(min = 2, max = 255) String title,
        @Positive double amount,
        @NotNull LocalDateTime dueDate
) {
}
