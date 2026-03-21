package com.example.autoservice.repository;

import com.example.autoservice.model.LicenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LicenseTypeRepository extends JpaRepository<LicenseType, UUID> {
}