package com.example.Easeplan.api.Calendar.repository;

import com.example.Easeplan.api.Calendar.domain.GoogleCalendarInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface GoogleCalendarRepository extends JpaRepository<GoogleCalendarInfo, String> {
    List<GoogleCalendarInfo> findByUserEmail(String email);
}