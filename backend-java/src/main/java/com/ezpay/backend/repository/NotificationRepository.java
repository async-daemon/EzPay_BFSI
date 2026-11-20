package com.ezpay.backend.repository;

import com.ezpay.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Notification> findByNotificationIdAndUserId(String notificationId, String userId);
}
