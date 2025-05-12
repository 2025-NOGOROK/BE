package com.example.Easeplan.global.auth.repository;


import java.util.Optional;

import com.example.Easeplan.global.auth.dto.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByEmail(String email);

    @Transactional
    @Modifying
    void deleteByEmail(String email);
}
