package com.ezpay.backend.service;

import com.ezpay.backend.dto.BankTransferRequest;
import com.ezpay.backend.dto.TransactionResponse;
import com.ezpay.backend.dto.UpiPaymentRequest;
import com.ezpay.backend.entity.BankAccount;
import com.ezpay.backend.entity.Transaction;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.exception.ApiException;
import com.ezpay.backend.repository.BankAccountRepository;
import com.ezpay.backend.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Use Case 2: Payment Processing (User stories 2.1 UPI, 2.2 Bank Transfer). */
@Service
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final FraudDetectionService fraudDetectionService;
    private final NotificationService notificationService;
    private final EncryptionService encryptionService;

    public PaymentService(
            TransactionRepository transactionRepository,
            BankAccountRepository bankAccountRepository,
            FraudDetectionService fraudDetectionService,
            NotificationService notificationService,
            EncryptionService encryptionService
    ) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.fraudDetectionService = fraudDetectionService;
        this.notificationService = notificationService;
        this.encryptionService = encryptionService;
    }

    // noRollbackFor: a BLOCKED/REQUIRES_VERIFICATION transaction + its
    // notification must still be persisted even though the method then
    // throws ApiException to return the 202/403 response (matches the
    // Python build's "commit, then raise HTTPException" ordering).
    @Transactional(noRollbackFor = ApiException.class)
    public TransactionResponse payUpi(User user, UpiPaymentRequest payload) {
        if (payload.recipientUpiId() == null || !payload.recipientUpiId().contains("@")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid UPI ID");
        }

        FraudDetectionService.Verdict verdict = fraudDetectionService.assess(user, payload.amount());

        Transaction txn = new Transaction();
        txn.setUserId(user.getUserId());
        txn.setType("UPI");
        txn.setAmount(payload.amount());
        txn.setCounterparty(payload.recipientUpiId());
        txn.setNote(payload.note());

        if (verdict.suspicious()) {
            blockOrFlag(user, txn, verdict);
        }

        // Simulated UPI Service round-trip (BRD: "UPI Service processes the payment").
        txn.setStatus("COMPLETED");
        transactionRepository.save(txn);
        notificationService.notifyTransactionConfirmation(user, txn);
        return toResponse(txn);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TransactionResponse bankTransfer(User user, BankTransferRequest payload) {
        BankAccount sourceAccount = bankAccountRepository
                .findByAccountIdAndUserId(payload.fromAccountId(), user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Source bank account not found"));

        if (payload.recipientIfscCode() == null || payload.recipientIfscCode().length() != 11) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid recipient IFSC code");
        }

        FraudDetectionService.Verdict verdict = fraudDetectionService.assess(user, payload.amount());
        String maskedSource = encryptionService.decrypt(sourceAccount.getAccountNumber());
        maskedSource = maskedSource.substring(Math.max(0, maskedSource.length() - 4));

        String recipientAccountNumber = payload.recipientAccountNumber();
        String maskedRecipient = "*".repeat(Math.max(0, recipientAccountNumber.length() - 4))
                + recipientAccountNumber.substring(Math.max(0, recipientAccountNumber.length() - 4));

        Transaction txn = new Transaction();
        txn.setUserId(user.getUserId());
        txn.setType("BANK_TRANSFER");
        txn.setAmount(payload.amount());
        txn.setCounterparty(maskedRecipient + " (" + payload.recipientIfscCode() + ")");
        txn.setNote(((payload.purpose() != null ? payload.purpose() : "") + " [from ****" + maskedSource + "]").strip());

        if (verdict.suspicious()) {
            blockOrFlag(user, txn, verdict);
        }

        // Simulated Banking Service round-trip (BRD: "Banking Service processes the transfer").
        txn.setStatus("COMPLETED");
        transactionRepository.save(txn);
        notificationService.notifyTransactionConfirmation(user, txn);
        return toResponse(txn);
    }

    private void blockOrFlag(User user, Transaction txn, FraudDetectionService.Verdict verdict) {
        txn.setStatus(verdict.requiresAdditionalVerification() ? "REQUIRES_VERIFICATION" : "BLOCKED");
        txn.setFailureReason(verdict.reason());
        transactionRepository.save(txn);
        notificationService.notifyTransactionConfirmation(user, txn);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("reason", verdict.reason());
        detail.put("transactionId", txn.getTransactionId());
        detail.put("status", txn.getStatus());
        throw new ApiException(
                "REQUIRES_VERIFICATION".equals(txn.getStatus()) ? HttpStatus.ACCEPTED : HttpStatus.FORBIDDEN,
                detail
        );
    }

    static TransactionResponse toResponse(Transaction txn) {
        return new TransactionResponse(
                txn.getTransactionId(), txn.getType(), txn.getAmount(), txn.getCounterparty(),
                txn.getNote(), txn.getDate(), txn.getStatus(), txn.getFailureReason()
        );
    }
}
