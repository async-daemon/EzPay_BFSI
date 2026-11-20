package com.ezpay.backend.repository;

import com.ezpay.backend.entity.AuthenticationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthenticationSessionRepository extends JpaRepository<AuthenticationSession, String> {
    Optional<AuthenticationSession> findBySessionTokenAndValidIsTrue(String sessionToken);
}
