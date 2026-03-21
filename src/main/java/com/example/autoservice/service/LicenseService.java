package com.example.autoservice.service;

import com.example.autoservice.model.*;
import com.example.autoservice.repository.*;
import com.example.autoservice.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.HexFormat;

@Service
public class LicenseService {
    private final LicenseRepository licenseRepo;
    private final DeviceRepository deviceRepo;
    private final DeviceLicenseRepository devLicRepo;
    private final ProductRepository productRepo;
    private final LicenseTypeRepository typeRepo;
    private final LicenseHistoryRepository historyRepo;

    @Value("${license.signature.secret}")
    private String signatureSecret;

    public LicenseService(LicenseRepository lr, DeviceRepository dr, DeviceLicenseRepository dlr,
                          ProductRepository pr, LicenseTypeRepository tr, LicenseHistoryRepository hr) {
        this.licenseRepo = lr;
        this.deviceRepo = dr;
        this.devLicRepo = dlr;
        this.productRepo = pr;
        this.typeRepo = tr;
        this.historyRepo = hr;
    }

    private void recordHistory(License license, User user, String status, String description) {
        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setUser(user);
        history.setStatus(status);
        history.setChange_date(Instant.now());
        history.setDescription(description);
        historyRepo.save(history);
    }

    @Transactional
    public License createLicense(CreateLicenseRequest request, User admin) {
        Product product = productRepo.findById(request.productId)
                .orElseThrow(() -> new RuntimeException("404: Product not found"));

        LicenseType type = typeRepo.findById(request.licenseTypeId)
                .orElseThrow(() -> new RuntimeException("404: License type not found"));

        License license = new License();
        license.setCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        license.setProduct(product);
        license.setType(type);
        license.setDevice_count(2);
        license.setEnding_date(Instant.now().plus(type.getDefault_duration_in_days(), ChronoUnit.DAYS));

        License saved = licenseRepo.save(license);
        recordHistory(saved, admin, "CREATED", "License created by admin");
        return saved;
    }

    @Transactional
    public TicketResponse activate(String code, String mac, String devName, User user) {
        License license = licenseRepo.findByCode(code)
                .orElseThrow(() -> new RuntimeException("404: License not found"));

        if (license.getUser() != null && !license.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("403: License owned by another user");
        }

        if (license.getProduct().is_blocked() || license.isBlocked()) {
            throw new RuntimeException("403: License or product is blocked");
        }

        Device device = deviceRepo.findByMacAddress(mac).orElse(null);

        if (device == null || !devLicRepo.existsByLicenseAndDevice(license, device)) {
            long currentDevices = devLicRepo.countByLicense(license);
            if (currentDevices >= license.getDevice_count()) {
                throw new RuntimeException("409: Device limit reached");
            }

            if (device == null) {
                device = new Device();
                device.setMacAddress(mac);
                device.setName(devName);
                device.setUser(user);
                device = deviceRepo.save(device);
            }

            DeviceLicense link = new DeviceLicense();
            link.setLicense(license);
            link.setDevice(device);
            link.setActivation_date(Instant.now());
            devLicRepo.save(link);

            if (license.getFirst_activation_date() == null) {
                license.setFirst_activation_date(Instant.now());
                license.setUser(user);
                licenseRepo.save(license);
            }
            recordHistory(license, user, "ACTIVATED", "Activated on device: " + mac);
        }

        return buildResponse(license, device);
    }

    @Transactional
    public TicketResponse renew(String code, User user) {
        License license = licenseRepo.findByCode(code)
                .orElseThrow(() -> new RuntimeException("404: License not found"));

        if (license.getProduct().is_blocked() || license.isBlocked()) {
            throw new RuntimeException("403: Renewal impossible: blocked status");
        }

        Instant now = Instant.now();
        Instant expiration = license.getEnding_date();

        if (expiration != null && expiration.isAfter(now)) {
            long daysRemaining = ChronoUnit.DAYS.between(now, expiration);
            if (daysRemaining > 7) {
                throw new RuntimeException("409: Renewal allowed only when 7 or fewer days remain");
            }
        }

        Instant baseDate = (expiration == null || expiration.isBefore(now)) ? now : expiration;
        license.setEnding_date(baseDate.plus(license.getType().getDefault_duration_in_days(), ChronoUnit.DAYS));

        licenseRepo.save(license);
        recordHistory(license, user, "RENEWED", "License extended");

        return buildResponse(license, null);
    }

    public TicketResponse verifyLicense(String deviceMac, String licenseCode) {
        License license = licenseRepo.findByCode(licenseCode)
                .orElseThrow(() -> new RuntimeException("404: License not found"));

        Device device = deviceRepo.findByMacAddress(deviceMac)
                .orElseThrow(() -> new RuntimeException("404: Device not found"));

        if (!devLicRepo.existsByLicenseAndDevice(license, device)) {
            throw new RuntimeException("403: License not activated on this device");
        }

        return buildResponse(license, device);
    }

    private TicketResponse buildResponse(License l, Device d) {
        Ticket t = new Ticket();
        t.serverDate = Instant.now();
        t.activationDate = l.getFirst_activation_date();
        t.expirationDate = l.getEnding_date();
        t.userId = (l.getUser() != null) ? l.getUser().getId() : null;
        t.deviceId = (d != null) ? d.getId() : null;
        t.blocked = l.isBlocked();
        t.ticketLifetime = 3600;

        TicketResponse resp = new TicketResponse();
        resp.ticket = t;

        // Формируем данные для подписи
        String mac = (d != null) ? d.getMacAddress() : "null";
        String dataToSign = l.getCode() + ":" + mac + ":" + t.expirationDate;

        // Вычисляем HMAC-SHA256 подпись
        resp.signature = calculateHmacSha256(dataToSign);

        return resp;
    }

    private String calculateHmacSha256(String data) {
        try {
            // Используем секрет из application.properties
            byte[] keyBytes = signatureSecret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);

            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Ручной перевод байтов в HEX-строку (вместо HexFormat)
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    public TicketResponse activateLicense(ActivateLicenseRequest request, User user) {
        return activate(request.licenseCode, request.deviceMac, request.deviceName, user);
    }

    public TicketResponse extendLicense(ExtendLicenseRequest request, User user) {
        return renew(request.licenseCode, user);
    }
}