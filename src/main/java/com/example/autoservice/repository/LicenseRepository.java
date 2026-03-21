package com.example.autoservice.repository;

import com.example.autoservice.model.License;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LicenseRepository extends JpaRepository<License, UUID> {
    Optional<License> findByCode(String code);
}