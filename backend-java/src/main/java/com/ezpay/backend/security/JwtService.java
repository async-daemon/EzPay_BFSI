package com.ezpay.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** JWT create/decode — mirrors {@code app/security.py}'s python-jose usage. */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expireMinutes;

    public JwtService(
            @Value("${ezpay.jwt.secret}") String secret,
            @Value("${ezpay.jwt.access-token-expire-minutes}") long expireMinutes
    ) {
        // HS256 needs >= 32 bytes; pad a short dev secret rather than failing at startup.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireMinutes = expireMinutes;
    }

    public String createAccessToken(String userId, String sessionToken) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMinutes * 60_000);
        return Jwts.builder()
                .subject(userId)
                .claim("sid", sessionToken)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims decode(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Could not validate credentials");
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
