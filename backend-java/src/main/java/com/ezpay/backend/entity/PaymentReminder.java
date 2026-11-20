package com.ezpay.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/** Maps to the ER diagram's {@code PaymentReminder} entity (User story 4.2). */
@Entity
@Table(name = "payment_reminders")
public class PaymentReminder {

    @Id
    @Column(name = "reminder_id", length = 36)
    private String reminderId = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private double amount;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    /** UPCOMING | DUE_SOON | OVERDUE | PAID */
    @Column(nullable = false, length = 20)
    private String status = "UPCOMING";

    public String getReminderId() { return reminderId; }
    public void setReminderId(String reminderId) { this.reminderId = reminderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
