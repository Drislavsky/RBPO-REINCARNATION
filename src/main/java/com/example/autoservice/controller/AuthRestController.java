package com.example.autoservice.controller;

import com.example.autoservice.model.User;
import com.example.autoservice.service.TokenService;
import com.example.autoservice.service.UserService;
import org.springframework.http.ResponseEntity;
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
            // Если роль не указана, ставим обычного пользователя
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
            // Используем UserService для поиска (через UserDetailsService)
            User user = (User) userService.loadUserByUsername(request.getUsername());

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid password"));
            }

            String token = tokenService.generateAccessToken(user);
            return ResponseEntity.ok(Map.of("accessToken", token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
    }
}