package com.example.autoservice.service;

import com.example.autoservice.model.*;
import com.example.autoservice.repository.*;
import com.example.autoservice.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Base64;

@Service
public class LicenseService {
    private final LicenseRepository licenseRepo;
    private final DeviceRepository deviceRepo;
    private final DeviceLicenseRepository devLicRepo;
    private final ProductRepository productRepo;
    private final LicenseTypeRepository typeRepo;
    private final LicenseHistoryRepository historyRepo;

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
        // Устанавливаем лимит, например, 2 или берем из запроса, если нужно
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

        // 1. Проверки владельца и блокировок
        if (license.getUser() != null && !license.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("403: License owned by another user");
        }

        if (license.getProduct().is_blocked() || license.isBlocked()) {
            throw new RuntimeException("403: License or product is blocked");
        }

        // 2. Ищем устройство, но НЕ сохраняем его сразу, если его нет
        Device device = deviceRepo.findByMacAddress(mac).orElse(null);

        // 3. Если устройство новое или не привязано к этой лицензии
        if (device == null || !devLicRepo.existsByLicenseAndDevice(license, device)) {

            // ПРОВЕРКА ЛИМИТА (важно: делаем до сохранения устройства)
            long currentDevices = devLicRepo.countByLicense(license);
            if (currentDevices >= license.getDevice_count()) {
                throw new RuntimeException("409: Device limit reached (" + license.getDevice_count() + ")");
            }

            // Если устройства не было в базе вообще — создаем
            if (device == null) {
                device = new Device();
                device.setMacAddress(mac);
                device.setName(devName);
                device.setUser(user);
                device = deviceRepo.save(device);
            }

            // Создаем связь
            DeviceLicense link = new DeviceLicense();
            link.setLicense(license);
            link.setDevice(device);
            link.setActivation_date(Instant.now());
            devLicRepo.save(link);

            // Обновляем данные лицензии при первой активации
            if (license.getFirst_activation_date() == null) {
                license.setFirst_activation_date(Instant.now());
                license.setUser(user);
                licenseRepo.save(license);
            }
            recordHistory(license, user, "ACTIVATED", "Activated on device: " + mac);
        }

        return buildResponse(license, device);
    }

    @Transactional(readOnly = true)
    public TicketResponse verifyLicense(String deviceMac, String licenseCode) {
        License license = licenseRepo.findByCode(licenseCode)
                .orElseThrow(() -> new RuntimeException("404: License not found"));

        Device device = deviceRepo.findByMacAddress(deviceMac)
                .orElseThrow(() -> new RuntimeException("404: Device not found"));

        if (!devLicRepo.existsByLicenseAndDevice(license, device)) {
            throw new RuntimeException("403: License not activated on this device");
        }

        if (license.getProduct().is_blocked() || license.isBlocked() ||
                (license.getEnding_date() != null && license.getEnding_date().isBefore(Instant.now()))) {
            throw new RuntimeException("403: License invalid, expired or blocked");
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

        String mac = (d != null) ? d.getMacAddress() : "null";
        resp.signature = Base64.getEncoder().encodeToString(
                (l.getCode() + ":" + mac + ":" + t.expirationDate).getBytes()
        );

        return resp;
    }

    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request, User user) {
        return activate(request.licenseCode, request.deviceMac, request.deviceName, user);
    }

    @Transactional
    public TicketResponse extendLicense(ExtendLicenseRequest request, User user) {
        return renew(request.licenseCode, user);
    }
}