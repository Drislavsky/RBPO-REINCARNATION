package com.example.autoservice.controller;

import com.example.autoservice.model.User;
import com.example.autoservice.service.TokenService;
import com.example.autoservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UserService userService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthRestController(UserService userService, TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        try {
            String role = (request.getRole() == null || request.getRole().isEmpty())
                    ? "ROLE_USER" : request.getRole();

            User user = userService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    role
            );

            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully",
                    "username", user.getUsername()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = (User) userService.loadUserByUsername(request.getUsername());

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid password"));
            }

            return ResponseEntity.ok(tokenService.issueTokens(user));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
            }

            return ResponseEntity.ok(tokenService.refreshTokens(request.getRefreshToken()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token is invalid"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshRequest request) {
        try {
            if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
            }

            tokenService.revokeSessionByRefreshToken(request.getRefreshToken(), "Logout requested by user");
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Logout failed"));
        }
    }
}