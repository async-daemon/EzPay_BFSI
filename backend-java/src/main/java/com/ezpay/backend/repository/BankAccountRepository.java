package com.ezpay.backend.repository;

import com.ezpay.backend.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
    List<BankAccount> findByUserId(String userId);
    Optional<BankAccount> findByAccountIdAndUserId(String accountId, String userId);
}
