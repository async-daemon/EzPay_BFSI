package com.ezpay.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/** Maps to the ER diagram's {@code PasswordResetRequest} entity (User story 1.3). */
@Entity
@Table(name = "password_reset_requests")
public class PasswordResetRequest {

    @Id
    @Column(name = "request_id", length = 36)
    private String requestId = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "reset_token", nullable = false, length = 64)
    private String resetToken;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate = LocalDateTime.now().plusMinutes(30);

    @Column(nullable = false)
    private boolean used = false;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public LocalDateTime getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDateTime expirationDate) { this.expirationDate = expirationDate; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
