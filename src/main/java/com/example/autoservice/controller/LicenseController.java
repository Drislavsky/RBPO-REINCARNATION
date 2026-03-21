package com.example.autoservice.controller;

import com.example.autoservice.service.LicenseService;
import com.example.autoservice.repository.UserRepository;
import com.example.autoservice.model.User;
import com.example.autoservice.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/license")
public class LicenseController {
    private final LicenseService licenseService;
    private final UserRepository userRepository;

    public LicenseController(LicenseService licenseService, UserRepository userRepository) {
        this.licenseService = licenseService;
        this.userRepository = userRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody CreateLicenseRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(licenseService.createLicense(request, user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody ActivateLicenseRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            TicketResponse response = licenseService.activateLicense(request, user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String mac, @RequestParam String code) {
        try {
            TicketResponse response = licenseService.verifyLicense(mac, code);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/extend")
    public ResponseEntity<?> extend(@RequestBody ExtendLicenseRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            TicketResponse response = licenseService.extendLicense(request, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}