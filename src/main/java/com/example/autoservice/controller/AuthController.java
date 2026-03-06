package com.example.autoservice.controller;

import com.example.autoservice.model.User;
import com.example.autoservice.model.UserSession;
import com.example.autoservice.repository.UserRepository;
import com.example.autoservice.service.TokenService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        User user = userOpt.get();

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        UserSession session = tokenService.saveRefreshSession(user, refreshToken);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "sessionId", session.getId()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        String oldRefreshToken = request.getRefreshToken();

        try {
            tokenService.validateRefreshToken(oldRefreshToken);

            Optional<UserSession> sessionOpt = tokenService.findSessionByRefreshToken(oldRefreshToken);

            if (sessionOpt.isEmpty() || !sessionOpt.get().isValid()) {
                return ResponseEntity.status(401).body("Invalid or expired refresh token");
            }

            UserSession session = sessionOpt.get();
            tokenService.revokeSession(session);

            User user = session.getUser();

            String newAccessToken = tokenService.generateAccessToken(user);
            String newRefreshToken = tokenService.generateRefreshToken(user);

            UserSession newSession = tokenService.saveRefreshSession(user, newRefreshToken);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken,
                    "sessionId", newSession.getId()
            ));

        } catch (JwtException e) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
    }
}