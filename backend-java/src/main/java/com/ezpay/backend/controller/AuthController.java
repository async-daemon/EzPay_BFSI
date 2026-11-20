package com.ezpay.backend.controller;

import com.ezpay.backend.dto.*;
import com.ezpay.backend.entity.User;
import com.ezpay.backend.security.JwtService;
import com.ezpay.backend.service.AuthService;
import com.ezpay.backend.web.CurrentUser;
import com.ezpay.backend.web.CurrentUserArgumentResolver;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Use Case 1 / User Story 1.1 & 1.3: registration+login, password recovery. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest payload) {
        return authService.register(payload);
    }

    @PostMapping("/verify-otp")
    public TokenResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest payload) {
        return authService.verifyOtp(payload);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest payload) {
        return authService.login(payload);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CurrentUser User currentUser, HttpServletRequest request) {
        String token = CurrentUserArgumentResolver.extractBearerToken(request);
        Claims claims = jwtService.decode(token);
        authService.logout(currentUser, claims.get("sid", String.class));
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest payload) {
        return authService.forgotPassword(payload);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest payload) {
        authService.resetPassword(payload);
    }
}
