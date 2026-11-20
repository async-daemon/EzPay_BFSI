package com.ezpay.backend.service;

import com.ezpay.backend.dto.CreateReminderRequest;
import com.ezpay.backend.dto.ReminderResponse;
import com.ezpay.backend.entity.PaymentReminder;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.exception.ApiException;
import com.ezpay.backend.repository.PaymentReminderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Use Case 4 / User Story 4.2: Payment Reminders. */
@Service
public class ReminderService {

    private final PaymentReminderRepository reminderRepository;
    private final NotificationService notificationService;

    public ReminderService(PaymentReminderRepository reminderRepository, NotificationService notificationService) {
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReminderResponse create(User user, CreateReminderRequest payload) {
        PaymentReminder reminder = new PaymentReminder();
        reminder.setUserId(user.getUserId());
        reminder.setTitle(payload.title());
        reminder.setAmount(payload.amount());
        reminder.setDueDate(payload.dueDate());
        reminderRepository.save(reminder);
        return toResponse(reminder);
    }

    @Transactional
    public List<ReminderResponse> list(User user) {
        List<PaymentReminder> reminders = reminderRepository.findByUserIdOrderByDueDateAsc(user.getUserId());
        LocalDateTime now = LocalDateTime.now();
        for (PaymentReminder r : reminders) {
            if ("PAID".equals(r.getStatus())) {
                continue;
            }
            if (r.getDueDate().isBefore(now)) {
                r.setStatus("OVERDUE");
            } else if (!r.getDueDate().isAfter(now.plusDays(3))) {
                r.setStatus("DUE_SOON");
            } else {
                r.setStatus("UPCOMING");
            }
        }
        reminderRepository.saveAll(reminders);
        return reminders.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void sendNow(User user, String reminderId) {
        PaymentReminder reminder = reminderRepository.findByReminderIdAndUserId(reminderId, user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reminder not found"));
        notificationService.notifyPaymentReminder(user, reminder);
    }

    @Transactional
    public ReminderResponse markPaid(User user, String reminderId) {
        PaymentReminder reminder = reminderRepository.findByReminderIdAndUserId(reminderId, user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reminder not found"));
        reminder.setStatus("PAID");
        reminderRepository.save(reminder);
        return toResponse(reminder);
    }

    private ReminderResponse toResponse(PaymentReminder r) {
        return new ReminderResponse(r.getReminderId(), r.getTitle(), r.getAmount(), r.getDueDate(), r.getStatus());
    }
}
