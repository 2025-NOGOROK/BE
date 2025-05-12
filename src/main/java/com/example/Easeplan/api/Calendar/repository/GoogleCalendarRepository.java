package com.example.Easeplan.api.Calendar.repository;

import com.example.Easeplan.api.Calendar.domain.GoogleCalendarInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoogleCalendarRepository extends JpaRepository<GoogleCalendarInfo, String> {
    List<GoogleCalendarInfo> findByUserEmail(String email);
}