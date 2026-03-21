package com.example.autoservice.repository;

import com.example.autoservice.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    // Внимание: название метода должно СТРОГО совпадать с полем в модели (macAddress)
    Optional<Device> findByMacAddress(String macAddress);
}