package com.example.autoservice.repository;

import com.example.autoservice.model.Device;
import com.example.autoservice.model.DeviceLicense;
import com.example.autoservice.model.License;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, UUID> {
    // Проверка наличия записи о связке лицензии и устройства
    boolean existsByLicenseAndDevice(License license, Device device);

    // Подсчет текущего кол-ва устройств для лицензии
    long countByLicense(License license);
}