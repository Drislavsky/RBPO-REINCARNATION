package com.example.autoservice.repository;

import com.example.autoservice.model.LicenseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, UUID> {
}