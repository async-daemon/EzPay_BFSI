package com.ezpay.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unified ledger entry backing TransactionHistory (User story 3.1/3.2).
 * Created alongside every UPI/BankTransfer payment so the user has a single
 * history/status view, matching the {@code Transaction} entity on the ER
 * diagram (transactionId, type, amount, date, status).
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "transaction_id", length = 36)
    private String transactionId = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /** "UPI" | "BANK_TRANSFER" */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false, length = 255)
    private String counterparty;

    @Column(length = 255)
    private String note;

    // Mapped to a non-"date" column name: DATE is a reserved word in Oracle
    // (ORA-03050), so the bare field name can't be used as the SQL column.
    @Column(name = "txn_date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    /** INITIATED | PROCESSING | COMPLETED | FAILED | BLOCKED | REQUIRES_VERIFICATION */
    @Column(nullable = false, length = 25)
    private String status = "INITIATED";

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
