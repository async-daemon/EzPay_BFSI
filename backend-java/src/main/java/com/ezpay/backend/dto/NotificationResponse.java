package com.ezpay.backend.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        String notificationId,
        String channel,
        String category,
        String message,
        LocalDateTime createdAt,
        boolean isRead
) {
}
