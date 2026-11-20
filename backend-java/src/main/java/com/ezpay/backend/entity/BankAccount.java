package com.ezpay.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

/** Backs {@code User.addBankAccount()} / {@code viewBankTransfers()} from the BRD class diagram. */
@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @Column(name = "account_id", length = 36)
    private String accountId = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "account_number", nullable = false, length = 255)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 20)
    private String ifscCode;

    @Column(name = "account_holder_name", nullable = false, length = 100)
    private String accountHolderName;

    @Column(name = "is_verified", nullable = false)
    private boolean verified = true;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
