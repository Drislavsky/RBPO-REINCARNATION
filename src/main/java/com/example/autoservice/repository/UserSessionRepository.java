package com.example.autoservice.repository;

import com.example.autoservice.model.SessionStatus;
import com.example.autoservice.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByLicenseKey(String licenseKey);

    Optional<UserSession> findByIdAndStatus(Long id, SessionStatus status);
}