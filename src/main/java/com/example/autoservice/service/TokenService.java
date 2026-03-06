package com.example.autoservice.service;

import com.example.autoservice.model.SessionStatus;
import com.example.autoservice.model.User;
import com.example.autoservice.model.UserSession;
import com.example.autoservice.repository.UserSessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class TokenService {

    @Value("${jwt.access.secret}")
    private String accessSecret;

    @Value("${jwt.refresh.secret}")
    private String refreshSecret;

    @Value("${jwt.access.expiration}")
    private long accessExpirationMs;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    private final UserSessionRepository sessionRepository;

    public TokenService(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @PostConstruct
    public void init() {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes());
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes());
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        claims.put("userId", user.getId());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(refreshKey)
                .compact();
    }

    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims validateRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(refreshKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Transactional
    public UserSession saveRefreshSession(User user, String refreshToken) {
        Instant expiresAt = Instant.now().plusMillis(refreshExpirationMs);
        UserSession session = new UserSession(user, refreshToken, expiresAt);
        return sessionRepository.save(session);
    }

    @Transactional
    public Optional<UserSession> findSessionByRefreshToken(String refreshToken) {
        return sessionRepository.findByRefreshToken(refreshToken);
    }

    @Transactional
    public void revokeSession(UserSession session) {
        session.revoke();
        sessionRepository.save(session);
    }
}