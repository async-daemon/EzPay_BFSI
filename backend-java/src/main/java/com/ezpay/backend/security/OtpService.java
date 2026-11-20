package com.ezpay.backend.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dev-mode in-memory OTP store (mobile verification for registration, User
 * Story 1.1 step 5-6). A production deployment would back this with a cache
 * (e.g. Redis) and a real SMS gateway instead of returning the code directly.
 */
@Component
public class OtpService {

    private static final long OTP_TTL_MILLIS = 300_000; // 5 minutes
    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpRecord> store = new ConcurrentHashMap<>();

    public String generate(String mobileNumber) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(mobileNumber, new OtpRecord(code, System.currentTimeMillis() + OTP_TTL_MILLIS));
        return code;
    }

    public boolean verify(String mobileNumber, String code) {
        OtpRecord record = store.get(mobileNumber);
        if (record == null) {
            return false;
        }
        if (System.currentTimeMillis() > record.expiresAtMillis()) {
            store.remove(mobileNumber);
            return false;
        }
        if (java.security.MessageDigest.isEqual(record.code().getBytes(), code.getBytes())) {
            store.remove(mobileNumber);
            return true;
        }
        return false;
    }

    private record OtpRecord(String code, long expiresAtMillis) {
    }
}
