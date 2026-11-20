package com.ezpay.backend.repository;

import com.ezpay.backend.entity.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, String> {
    Optional<PasswordResetRequest> findByResetTokenAndUsedIsFalse(String resetToken);
}
