package com.ezpay.backend.repository;

import com.ezpay.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByUserIdOrderByDateDesc(String userId);
    Optional<Transaction> findByTransactionIdAndUserId(String transactionId, String userId);
    long countByUserIdAndDateAfter(String userId, LocalDateTime since);
}
