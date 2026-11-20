package com.ezpay.backend.service;

import com.ezpay.backend.dto.TransactionResponse;
import com.ezpay.backend.entity.Transaction;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.exception.ApiException;
import com.ezpay.backend.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** Use Case 3: Transaction Management (3.1 history, 3.2 status tracking). */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionResponse> history(
            User user, String type, String status, LocalDateTime dateFrom, LocalDateTime dateTo
    ) {
        return transactionRepository.findByUserIdOrderByDateDesc(user.getUserId()).stream()
                .filter(t -> type == null || t.getType().equalsIgnoreCase(type))
                .filter(t -> status == null || t.getStatus().equalsIgnoreCase(status))
                .filter(t -> dateFrom == null || !t.getDate().isBefore(dateFrom))
                .filter(t -> dateTo == null || !t.getDate().isAfter(dateTo))
                .map(PaymentService::toResponse)
                .toList();
    }

    public TransactionResponse track(User user, String transactionId) {
        Transaction txn = transactionRepository.findByTransactionIdAndUserId(transactionId, user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
        return PaymentService.toResponse(txn);
    }
}
