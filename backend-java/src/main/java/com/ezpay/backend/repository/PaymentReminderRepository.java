package com.ezpay.backend.repository;

import com.ezpay.backend.entity.PaymentReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentReminderRepository extends JpaRepository<PaymentReminder, String> {
    List<PaymentReminder> findByUserIdOrderByDueDateAsc(String userId);
    Optional<PaymentReminder> findByReminderIdAndUserId(String reminderId, String userId);
}
