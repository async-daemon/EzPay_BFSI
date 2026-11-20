package com.ezpay.backend.service;

import com.ezpay.backend.entity.User;
import com.ezpay.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * User Story 5.2: Fraud Prevention. Simple, explainable rule-based checks
 * (the BRD gives no ML/vendor requirement). Returns a verdict so callers can
 * block or flag-and-continue the transaction.
 */
@Service
public class FraudDetectionService {

    public static final double HIGH_VALUE_THRESHOLD = 200_000.0;
    public static final int VELOCITY_WINDOW_MINUTES = 10;
    public static final int VELOCITY_MAX_TRANSACTIONS = 5;

    private final TransactionRepository transactionRepository;

    public FraudDetectionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public record Verdict(boolean suspicious, boolean requiresAdditionalVerification, String reason) {
        static Verdict clean() {
            return new Verdict(false, false, null);
        }
    }

    public Verdict assess(User user, double amount) {
        if (amount <= 0) {
            return new Verdict(true, false, "Transaction amount must be positive.");
        }

        if (amount >= HIGH_VALUE_THRESHOLD) {
            return new Verdict(
                    true,
                    true,
                    String.format("Amount exceeds the high-value threshold of ₹%,.0f.", HIGH_VALUE_THRESHOLD)
            );
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);
        long recentCount = transactionRepository.countByUserIdAndDateAfter(user.getUserId(), windowStart);
        if (recentCount >= VELOCITY_MAX_TRANSACTIONS) {
            return new Verdict(
                    true,
                    true,
                    String.format(
                            "More than %d transactions in the last %d minutes.",
                            VELOCITY_MAX_TRANSACTIONS, VELOCITY_WINDOW_MINUTES
                    )
            );
        }

        return Verdict.clean();
    }
}
