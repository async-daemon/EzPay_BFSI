package com.ezpay.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps to the ER diagram's {@code Authentication} entity (an active login session).
 * Named {@code AuthenticationSession} in Java to avoid clashing with
 * {@code java.security.Authentication} / Spring Security's own type.
 */
@Entity
@Table(name = "authentication")
public class AuthenticationSession {

    @Id
    @Column(name = "session_token", length = 36)
    private String sessionToken = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime = LocalDateTime.now();

    @Column(name = "is_valid", nullable = false)
    private boolean valid = true;

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
}
