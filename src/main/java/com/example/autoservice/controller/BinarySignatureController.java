package com.example.autoservice.controller;

import com.example.autoservice.binary.BinarySignaturePackage;
import com.example.autoservice.binary.BinarySignaturePackageService;
import com.example.autoservice.binary.MultipartMixedResponseFactory;
import com.example.autoservice.dto.MalwareSignatureIdsRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/binary/signatures")
public class BinarySignatureController {

    private static final MediaType MULTIPART_MIXED = MediaType.parseMediaType("multipart/mixed");

    private final BinarySignaturePackageService binarySignaturePackageService;
    private final MultipartMixedResponseFactory multipartMixedResponseFactory;

    public BinarySignatureController(BinarySignaturePackageService binarySignaturePackageService,
                                     MultipartMixedResponseFactory multipartMixedResponseFactory) {
        this.binarySignaturePackageService = binarySignaturePackageService;
        this.multipartMixedResponseFactory = multipartMixedResponseFactory;
    }

    @GetMapping(value = "/full", produces = "multipart/mixed")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<MultiValueMap<String, Object>> getFull() {
        BinarySignaturePackage binaryPackage = binarySignaturePackageService.buildFullPackage();
        return multipartResponse(binaryPackage);
    }

    @GetMapping(value = "/increment", produces = "multipart/mixed")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> getIncrement(@RequestParam(required = false) String since) {
        try {
            if (since == null || since.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parameter 'since' is required"));
            }

            Instant parsedSince = Instant.parse(since);
            BinarySignaturePackage binaryPackage = binarySignaturePackageService.buildIncrementPackage(parsedSince);
            return multipartResponse(binaryPackage);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid 'since' format. Use ISO-8601 instant"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/by-ids", produces = "multipart/mixed")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> getByIds(@RequestBody MalwareSignatureIdsRequest request) {
        try {
            List<UUID> ids = request != null ? request.getIds() : null;
            BinarySignaturePackage binaryPackage = binarySignaturePackageService.buildByIdsPackage(ids);
            return multipartResponse(binaryPackage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<MultiValueMap<String, Object>> multipartResponse(BinarySignaturePackage binaryPackage) {
        MultiValueMap<String, Object> body = multipartMixedResponseFactory.create(
                binaryPackage.getManifestBytes(),
                binaryPackage.getDataBytes()
        );
        return ResponseEntity.ok()
                .contentType(MULTIPART_MIXED)
                .body(body);
    }
}