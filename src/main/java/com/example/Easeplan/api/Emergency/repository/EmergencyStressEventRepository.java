package com.example.Easeplan.api.Emergency.repository;

import com.example.Easeplan.api.Emergency.domain.EmergencyStressEvent;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmergencyStressEventRepository extends JpaRepository<EmergencyStressEvent, Long> {
    Optional<EmergencyStressEvent> findTopByUserAndStatusOrderByCreatedAtDesc(User user, EmergencyStressEvent.Status status);
}
