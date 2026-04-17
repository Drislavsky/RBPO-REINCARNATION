package com.example.autoservice.repository;

import com.example.autoservice.model.TokenHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenHistoryRepository extends JpaRepository<TokenHistory, Long> {
    Optional<TokenHistory> findByTokenId(String tokenId);
}