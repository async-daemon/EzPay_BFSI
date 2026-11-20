package com.ezpay.backend.service;

import com.ezpay.backend.dto.*;
import com.ezpay.backend.entity.AuthenticationSession;
import com.ezpay.backend.entity.PasswordResetRequest;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.exception.ApiException;
import com.ezpay.backend.repository.AuthenticationSessionRepository;
import com.ezpay.backend.repository.PasswordResetRequestRepository;
import com.ezpay.backend.repository.UserRepository;
import com.ezpay.backend.security.JwtService;
import com.ezpay.backend.security.OtpService;
import com.ezpay.backend.security.PasswordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/** Use Case 1 / User Story 1.1 & 1.3: registration+login, password recovery. */
@Service
public class AuthService {

    // Set to false once a real SMS/email provider is wired up; while true the
    // OTP/reset token are echoed back in the API response so the app is
    // usable without an external gateway.
    private static final boolean DEV_MODE = true;

    private final UserRepository userRepository;
    private final AuthenticationSessionRepository sessionRepository;
    private final PasswordResetRequestRepository resetRequestRepository;
    private final PasswordService passwordService;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            AuthenticationSessionRepository sessionRepository,
            PasswordResetRequestRepository resetRequestRepository,
            PasswordService passwordService,
            OtpService otpService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.resetRequestRepository = resetRequestRepository;
        this.passwordService = passwordService;
        this.otpService = otpService;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest payload) {
        if (userRepository.existsByEmailOrMobileNumber(payload.email(), payload.mobileNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email or mobile number already registered");
        }

        User user = new User();
        user.setUsername(payload.username());
        user.setEmail(payload.email());
        user.setMobileNumber(payload.mobileNumber());
        user.setPassword(passwordService.hash(payload.password()));
        userRepository.save(user);

        String otp = otpService.generate(payload.mobileNumber());
        return new RegisterResponse(
                "Account created. Enter the verification code sent to your mobile number to activate login.",
                payload.mobileNumber(),
                DEV_MODE ? otp : null
        );
    }

    @Transactional
    public TokenResponse verifyOtp(VerifyOtpRequest payload) {
        if (!otpService.verify(payload.mobileNumber(), payload.otp())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
        }
        User user = userRepository.findByMobileNumber(payload.mobileNumber())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return issueSession(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest payload) {
        User user = userRepository.findByEmailOrMobileNumber(payload.identifier(), payload.identifier())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordService.verify(payload.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is inactive");
        }
        return issueSession(user);
    }

    private TokenResponse issueSession(User user) {
        AuthenticationSession session = new AuthenticationSession();
        session.setUserId(user.getUserId());
        sessionRepository.save(session);

        String token = jwtService.createAccessToken(user.getUserId(), session.getSessionToken());
        return new TokenResponse(token, user.getUserId(), user.getUsername());
    }

    @Transactional
    public void logout(User user, String sessionToken) {
        sessionRepository.findBySessionTokenAndValidIsTrue(sessionToken).ifPresent(session -> {
            session.setValid(false);
            sessionRepository.save(session);
        });
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest payload) {
        String genericMessage = "If that account exists, a password reset link has been sent.";
        var userOpt = userRepository.findByEmailOrMobileNumber(payload.identifier(), payload.identifier());
        if (userOpt.isEmpty()) {
            // Always return the same response to avoid leaking which identifiers are registered.
            return new ForgotPasswordResponse(genericMessage, null);
        }

        String resetToken = generateResetToken();
        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setUserId(userOpt.get().getUserId());
        resetRequest.setResetToken(resetToken);
        resetRequestRepository.save(resetRequest);

        return new ForgotPasswordResponse(genericMessage, DEV_MODE ? resetToken : null);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest payload) {
        PasswordResetRequest resetRequest = resetRequestRepository
                .findByResetTokenAndUsedIsFalse(payload.resetToken())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Reset link is invalid or has expired"));
        if (resetRequest.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset link is invalid or has expired");
        }

        User user = userRepository.findById(resetRequest.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPassword(passwordService.hash(payload.newPassword()));
        resetRequest.setUsed(true);
        userRepository.save(user);
        resetRequestRepository.save(resetRequest);
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
