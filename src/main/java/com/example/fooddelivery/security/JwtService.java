package com.example.fooddelivery.security;

import com.example.fooddelivery.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for generating, signing, parsing, and validating JSON Web Tokens (JWT).
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs
    ) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT Secret key must be configured and cannot be empty");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT Secret key must be at least 256 bits (32 bytes) long for HMAC-SHA256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT token containing subject (email), user ID, and role.
     *
     * @param userDetails Spring Security user details
     * @param userId      Database primary key ID
     * @param role        User domain role
     * @return Compact serialized JWT token string
     */
    public String generateToken(UserDetails userDetails, Long userId, Role role) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userId != null) {
            extraClaims.put("userId", userId);
        }
        if (role != null) {
            extraClaims.put("role", role.name());
        }
        return buildToken(extraClaims, userDetails.getUsername(), expirationMs);
    }

    /**
     * Builds and signs a token with custom claims, subject, and expiration.
     */
    public String buildToken(Map<String, Object> claims, String subject, long expirationDurationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationDurationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extracts username (email subject) from token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts user ID custom claim from token.
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Extracts role custom claim from token.
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Generic claims extraction helper.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validates that the token signature is authentic, not expired, and belongs to the supplied UserDetails.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username != null &&
                    username.equalsIgnoreCase(userDetails.getUsername()) &&
                    !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if the token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public long getExpirationTimeMs() {
        return expirationMs;
    }

    /**
     * Parses and cryptographically verifies the token against the HMAC-SHA256 signing key.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
