package com.example.autoservice.repository;

import com.example.autoservice.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByRefreshToken(String refreshToken);
    void deleteByUserIdAndStatus(Long userId, com.example.autoservice.model.SessionStatus status);
}