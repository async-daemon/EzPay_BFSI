package com.ezpay.backend.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** bcrypt hashing — mirrors {@code app/security.py}'s hash_password/verify_password. */
@Component
public class PasswordService {

    // bcrypt only uses the first 72 bytes of input; longer passwords are
    // truncated (matches RegisterRequest's max size of 128 chars).
    private static final int BCRYPT_MAX_BYTES = 72;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(truncate(rawPassword));
    }

    public boolean verify(String rawPassword, String hashed) {
        return encoder.matches(truncate(rawPassword), hashed);
    }

    private String truncate(String password) {
        byte[] bytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length <= BCRYPT_MAX_BYTES) {
            return password;
        }
        return new String(bytes, 0, BCRYPT_MAX_BYTES, java.nio.charset.StandardCharsets.UTF_8);
    }
}
