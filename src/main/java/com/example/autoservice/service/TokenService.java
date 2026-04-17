package com.example.autoservice.service;

import com.example.autoservice.model.*;
import com.example.autoservice.repository.TokenHistoryRepository;
import com.example.autoservice.repository.UserSessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    private final UserSessionRepository userSessionRepository;
    private final TokenHistoryRepository tokenHistoryRepository;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    public TokenService(UserSessionRepository userSessionRepository,
                        TokenHistoryRepository tokenHistoryRepository) {
        this.userSessionRepository = userSessionRepository;
        this.tokenHistoryRepository = tokenHistoryRepository;
    }

    @PostConstruct
    public void init() {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public Map<String, Object> issueTokens(User user) {
        Instant now = Instant.now();

        UserSession session = new UserSession();
        session.setUser(user);
        session.setStatus(SessionStatus.ACTIVE);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session = userSessionRepository.save(session);

        TokenBundle tokenBundle = buildTokenBundle(user, session.getId(), now);
        applyTokenBundle(session, user, tokenBundle, null);

        return buildResponse(tokenBundle, session.getId());
    }

    @Transactional
    public Map<String, Object> refreshTokens(String refreshToken) {
        Claims claims = validateRefreshToken(refreshToken);
        Long sessionId = extractSessionId(claims);
        String refreshTokenId = extractTokenId(claims);

        UserSession session = userSessionRepository.findByIdAndStatus(sessionId, SessionStatus.ACTIVE)
                .orElseThrow(() -> new BadCredentialsException("Session is not active"));

        validateStoredRefreshToken(session, refreshTokenId);

        Instant now = Instant.now();
        if (session.getRefreshExpiresAt() != null && session.getRefreshExpiresAt().isBefore(now)) {
            expireSession(session, "Refresh token expired");
            throw new BadCredentialsException("Refresh token expired");
        }

        markCurrentTokensAsRotated(session, now);

        TokenBundle tokenBundle = buildTokenBundle(session.getUser(), session.getId(), now);
        applyTokenBundle(session, session.getUser(), tokenBundle, now);

        return buildResponse(tokenBundle, session.getId());
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

    public Claims parseRefreshClaimsAllowExpired(String token) {
        try {
            return validateRefreshToken(token);
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    public boolean isAccessTokenActive(Claims claims) {
        if (claims == null) {
            return false;
        }

        Long sessionId = extractSessionId(claims);
        String tokenId = extractTokenId(claims);
        String tokenType = claims.get("typ", String.class);

        if (!"access".equals(tokenType)) {
            return false;
        }

        Optional<UserSession> sessionOptional = userSessionRepository.findById(sessionId);
        if (sessionOptional.isEmpty()) {
            return false;
        }

        UserSession session = sessionOptional.get();
        if (session.getStatus() != SessionStatus.ACTIVE) {
            return false;
        }

        if (!tokenId.equals(session.getCurrentAccessTokenId())) {
            return false;
        }

        Instant now = Instant.now();
        if (session.getAccessExpiresAt() != null && session.getAccessExpiresAt().isBefore(now)) {
            markTokenStatusIfPresent(tokenId, TokenHistoryStatus.EXPIRED, "Access token expired", now);
            return false;
        }

        return true;
    }

    @Transactional
    public void revokeSessionByRefreshToken(String refreshToken, String reason) {
        Claims claims = parseRefreshClaimsAllowExpired(refreshToken);
        if (claims == null) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        Long sessionId = extractSessionId(claims);
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BadCredentialsException("Session not found"));

        revokeSession(session, reason);
    }

    private void validateStoredRefreshToken(UserSession session, String refreshTokenId) {
        if (session.getCurrentRefreshTokenId() == null || !session.getCurrentRefreshTokenId().equals(refreshTokenId)) {
            throw new BadCredentialsException("Refresh token is revoked or replaced");
        }
    }

    private TokenBundle buildTokenBundle(User user, Long sessionId, Instant now) {
        String accessTokenId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();

        Instant accessExpiresAt = now.plusMillis(accessExpirationMs);
        Instant refreshExpiresAt = now.plusMillis(refreshExpirationMs);

        String accessToken = Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole())
                .claim("sid", sessionId)
                .claim("tid", accessTokenId)
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiresAt))
                .signWith(accessKey)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole())
                .claim("sid", sessionId)
                .claim("tid", refreshTokenId)
                .claim("typ", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshExpiresAt))
                .signWith(refreshKey)
                .compact();

        return new TokenBundle(accessToken, refreshToken, accessTokenId, refreshTokenId, accessExpiresAt, refreshExpiresAt, now);
    }

    private void applyTokenBundle(UserSession session, User user, TokenBundle tokenBundle, Instant statusChangeTime) {
        session.setCurrentAccessTokenId(tokenBundle.accessTokenId());
        session.setCurrentRefreshTokenId(tokenBundle.refreshTokenId());
        session.setAccessExpiresAt(tokenBundle.accessExpiresAt());
        session.setRefreshExpiresAt(tokenBundle.refreshExpiresAt());
        session.setStatus(SessionStatus.ACTIVE);
        session.setRevokedAt(null);
        session.setUpdatedAt(tokenBundle.issuedAt());
        userSessionRepository.save(session);

        saveHistory(session, user, tokenBundle.accessTokenId(), TokenType.ACCESS,
                TokenHistoryStatus.ACTIVATED, tokenBundle.issuedAt(), tokenBundle.accessExpiresAt(), statusChangeTime,
                statusChangeTime == null ? "Initial login" : "Token rotated");

        saveHistory(session, user, tokenBundle.refreshTokenId(), TokenType.REFRESH,
                TokenHistoryStatus.ACTIVATED, tokenBundle.issuedAt(), tokenBundle.refreshExpiresAt(), statusChangeTime,
                statusChangeTime == null ? "Initial login" : "Token rotated");
    }

    private void markCurrentTokensAsRotated(UserSession session, Instant now) {
        if (session.getCurrentAccessTokenId() != null) {
            markTokenStatusIfPresent(session.getCurrentAccessTokenId(), TokenHistoryStatus.REVOKED, "Access token rotated", now);
        }
        if (session.getCurrentRefreshTokenId() != null) {
            markTokenStatusIfPresent(session.getCurrentRefreshTokenId(), TokenHistoryStatus.REFRESHED, "Refresh token rotated", now);
        }
    }

    private void expireSession(UserSession session, String reason) {
        session.setStatus(SessionStatus.EXPIRED);
        session.setRevokedAt(Instant.now());
        userSessionRepository.save(session);

        if (session.getCurrentAccessTokenId() != null) {
            markTokenStatusIfPresent(session.getCurrentAccessTokenId(), TokenHistoryStatus.EXPIRED, reason, Instant.now());
        }
        if (session.getCurrentRefreshTokenId() != null) {
            markTokenStatusIfPresent(session.getCurrentRefreshTokenId(), TokenHistoryStatus.EXPIRED, reason, Instant.now());
        }
    }

    private void revokeSession(UserSession session, String reason) {
        Instant now = Instant.now();
        session.setStatus(SessionStatus.REVOKED);
        session.setRevokedAt(now);
        session.setUpdatedAt(now);
        userSessionRepository.save(session);

        if (session.getCurrentAccessTokenId() != null) {
            markTokenStatusIfPresent(session.getCurrentAccessTokenId(), TokenHistoryStatus.REVOKED, reason, now);
        }
        if (session.getCurrentRefreshTokenId() != null) {
            markTokenStatusIfPresent(session.getCurrentRefreshTokenId(), TokenHistoryStatus.REVOKED, reason, now);
        }
    }

    private void saveHistory(UserSession session,
                             User user,
                             String tokenId,
                             TokenType tokenType,
                             TokenHistoryStatus status,
                             Instant issuedAt,
                             Instant expiresAt,
                             Instant changedAt,
                             String reason) {
        TokenHistory history = new TokenHistory();
        history.setSession(session);
        history.setUser(user);
        history.setTokenId(tokenId);
        history.setTokenType(tokenType);
        history.setStatus(status);
        history.setIssuedAt(issuedAt);
        history.setExpiresAt(expiresAt);
        history.setChangedAt(changedAt);
        history.setReason(reason);
        tokenHistoryRepository.save(history);
    }

    private void markTokenStatusIfPresent(String tokenId, TokenHistoryStatus status, String reason, Instant changedAt) {
        tokenHistoryRepository.findByTokenId(tokenId).ifPresent(history -> {
            history.setStatus(status);
            history.setChangedAt(changedAt);
            history.setReason(reason);
            tokenHistoryRepository.save(history);
        });
    }

    private Map<String, Object> buildResponse(TokenBundle tokenBundle, Long sessionId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", tokenBundle.accessToken());
        response.put("refreshToken", tokenBundle.refreshToken());
        response.put("tokenType", "Bearer");
        response.put("accessExpiresAt", tokenBundle.accessExpiresAt().toString());
        response.put("refreshExpiresAt", tokenBundle.refreshExpiresAt().toString());
        response.put("sessionId", sessionId);
        return response;
    }

    private Long extractSessionId(Claims claims) {
        Number sid = claims.get("sid", Number.class);
        if (sid == null) {
            throw new BadCredentialsException("Session id is missing in token");
        }
        return sid.longValue();
    }

    private String extractTokenId(Claims claims) {
        String tokenId = claims.get("tid", String.class);
        if (tokenId == null || tokenId.isBlank()) {
            throw new BadCredentialsException("Token id is missing in token");
        }
        return tokenId;
    }

    private record TokenBundle(
            String accessToken,
            String refreshToken,
            String accessTokenId,
            String refreshTokenId,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            Instant issuedAt
    ) {
    }
}