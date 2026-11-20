package com.ezpay.backend.controller;

import com.ezpay.backend.dto.NotificationResponse;
import com.ezpay.backend.entity.Notification;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.service.NotificationService;
import com.ezpay.backend.web.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Use Case 4: Notification System — in-app notification center. */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@CurrentUser User currentUser) {
        return notificationService.list(currentUser).stream().map(this::toResponse).toList();
    }

    @PostMapping("/{notificationId}/read")
    public NotificationResponse markRead(@CurrentUser User currentUser, @PathVariable String notificationId) {
        return toResponse(notificationService.markRead(currentUser, notificationId));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getNotificationId(), n.getChannel(), n.getCategory(), n.getMessage(), n.getCreatedAt(), n.isRead()
        );
    }
}
