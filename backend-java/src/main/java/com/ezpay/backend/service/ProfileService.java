package com.ezpay.backend.service;

import com.ezpay.backend.dto.*;
import com.ezpay.backend.entity.BankAccount;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.exception.ApiException;
import com.ezpay.backend.repository.BankAccountRepository;
import com.ezpay.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Use Case 1 / User Story 1.2: Profile Management. */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final EncryptionService encryptionService;

    public ProfileService(
            UserRepository userRepository,
            BankAccountRepository bankAccountRepository,
            EncryptionService encryptionService
    ) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.encryptionService = encryptionService;
    }

    public UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getUserId(), user.getUsername(), user.getEmail(),
                user.getMobileNumber(), user.getUpiId(), user.getRegistrationDate()
        );
    }

    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest payload) {
        if (payload.username() != null && !payload.username().isBlank()) {
            user.setUsername(payload.username());
        }
        if (payload.upiId() != null && !payload.upiId().isBlank()) {
            user.setUpiId(payload.upiId());
        }
        userRepository.save(user);
        return toProfile(user);
    }

    @Transactional
    public UserProfileResponse addUpiId(User user, AddUpiIdRequest payload) {
        userRepository.findByUpiId(payload.upiId()).ifPresent(existing -> {
            if (!existing.getUserId().equals(user.getUserId())) {
                throw new ApiException(HttpStatus.CONFLICT, "UPI ID already in use");
            }
        });
        user.setUpiId(payload.upiId());
        userRepository.save(user);
        return toProfile(user);
    }

    @Transactional
    public BankAccountResponse addBankAccount(User user, AddBankAccountRequest payload) {
        BankAccount account = new BankAccount();
        account.setUserId(user.getUserId());
        account.setAccountNumber(encryptionService.encrypt(payload.accountNumber()));
        account.setIfscCode(payload.ifscCode().toUpperCase());
        account.setAccountHolderName(payload.accountHolderName());
        bankAccountRepository.save(account);

        return new BankAccountResponse(
                account.getAccountId(),
                encryptionService.maskAccountNumber(payload.accountNumber()),
                account.getIfscCode(),
                account.getAccountHolderName(),
                account.isVerified()
        );
    }

    public List<BankAccountResponse> listBankAccounts(User user) {
        return bankAccountRepository.findByUserId(user.getUserId()).stream()
                .map(a -> new BankAccountResponse(
                        a.getAccountId(),
                        encryptionService.maskAccountNumber(encryptionService.decrypt(a.getAccountNumber())),
                        a.getIfscCode(),
                        a.getAccountHolderName(),
                        a.isVerified()
                ))
                .toList();
    }
}
