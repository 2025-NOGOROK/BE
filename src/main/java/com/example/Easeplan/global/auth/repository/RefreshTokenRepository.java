package com.example.Easeplan.global.auth.repository;


import java.util.Optional;

import com.example.Easeplan.global.auth.dto.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByEmail(String email);
}
