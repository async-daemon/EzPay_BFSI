package com.ezpay.backend.repository;

import com.ezpay.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmailOrMobileNumber(String email, String mobileNumber);
    Optional<User> findByMobileNumber(String mobileNumber);
    Optional<User> findByUpiId(String upiId);
    boolean existsByEmailOrMobileNumber(String email, String mobileNumber);
}
