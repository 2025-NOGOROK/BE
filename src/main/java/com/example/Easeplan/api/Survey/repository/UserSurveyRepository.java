package com.example.Easeplan.api.Survey.repository;

import com.example.Easeplan.api.Survey.domain.UserSurvey;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSurveyRepository extends JpaRepository<UserSurvey, Long> {
    Optional<UserSurvey> findByUser(User user);
}
