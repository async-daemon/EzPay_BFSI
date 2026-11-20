package com.ezpay.backend.service;

import com.ezpay.backend.entity.Notification;
import com.ezpay.backend.entity.PaymentReminder;
import com.ezpay.backend.entity.Transaction;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.exception.ApiException;
import com.ezpay.backend.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Use Case 4: Notification System (transaction confirmations + reminders,
 * plus the in-app notification center). Push/email delivery is mocked with a
 * log line and an in-app Notification row; swap {@link #dispatch} for a real
 * provider (FCM/APNs + SMTP) in production.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger("ezpay.notifications");
    private static final DateTimeFormatter DUE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    private void dispatch(String channel, User user, String message) {
        log.info("[{} -> {}] {}", channel.toUpperCase(Locale.ROOT), user.getEmail() != null ? user.getEmail() : user.getMobileNumber(), message);
    }

    public Notification notify(User user, String category, String message) {
        dispatch("push", user, message);
        dispatch("email", user, message);

        Notification notification = new Notification();
        notification.setUserId(user.getUserId());
        notification.setChannel("push+email");
        notification.setCategory(category);
        notification.setMessage(message);
        return notificationRepository.save(notification);
    }

    public void notifyTransactionConfirmation(User user, Transaction txn) {
        String message = String.format(
                "Your %s of ₹%,.2f to %s is %s. Txn ID: %s",
                txn.getType().replace('_', ' ').toLowerCase(Locale.ROOT),
                txn.getAmount(),
                txn.getCounterparty(),
                txn.getStatus().toLowerCase(Locale.ROOT),
                txn.getTransactionId()
        );
        notify(user, "confirmation", message);
    }

    public void notifyPaymentReminder(User user, PaymentReminder reminder) {
        String message = String.format(
                "Reminder: '%s' of ₹%,.2f is due on %s.",
                reminder.getTitle(),
                reminder.getAmount(),
                reminder.getDueDate().format(DUE_DATE_FORMAT)
        );
        notify(user, "reminder", message);
    }

    public List<Notification> list(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
    }

    public Notification markRead(User user, String notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUserId(notificationId, user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }
}
