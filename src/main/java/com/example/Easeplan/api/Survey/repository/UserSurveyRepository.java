package com.example.Easeplan.api.Survey.repository;

import com.example.Easeplan.api.Survey.domain.UserSurvey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSurveyRepository extends JpaRepository<UserSurvey, Long> {
}
