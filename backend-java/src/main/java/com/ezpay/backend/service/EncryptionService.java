package com.ezpay.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Maps to the ER/class diagram's EncryptionService (encryptData/decryptData).
 * Used to encrypt sensitive fields at rest (e.g. bank account numbers) per
 * the Security Requirements table. Java's equivalent of the Python build's
 * Fernet symmetric encryption: AES-256-GCM (authenticated encryption) with a
 * random 12-byte IV prefixed to each ciphertext.
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public EncryptionService(@Value("${ezpay.field-encryption-key}") String configuredKey) throws GeneralSecurityException {
        this.key = deriveKey(configuredKey);
    }

    private SecretKeySpec deriveKey(String configuredKey) throws GeneralSecurityException {
        String material = (configuredKey == null || configuredKey.isBlank())
                // Dev-time fallback so the app runs without extra setup; a real
                // deployment must set EZPAY_FIELD_ENCRYPTION_KEY (see .env.example).
                ? "dev-only-insecure-encryption-key-change-me"
                : configuredKey;
        byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(sha256, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv).put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt field", e);
        }
    }

    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherTextBase64);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt field", e);
        }
    }

    /** Display helper: never show a full account number back to the client. */
    public String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        int maskedLength = accountNumber.length() - 4;
        return "*".repeat(maskedLength) + accountNumber.substring(maskedLength);
    }
}
