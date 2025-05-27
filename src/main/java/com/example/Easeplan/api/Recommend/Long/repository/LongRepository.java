package com.example.Easeplan.api.Recommend.Long.repository;

import com.example.Easeplan.api.Calendar.domain.GoogleCalendarInfo;
import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Recommend.Long.dto.RecommendationOption;
import com.example.Easeplan.api.Recommend.Long.dto.UserChoice;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LongRepository {
    private final UserRepository userRepository;
    private final UserChoiceRepository userChoiceRepository;

    public LongRepository(UserRepository userRepository, UserChoiceRepository userChoiceRepository) {
        this.userRepository = userRepository;
        this.userChoiceRepository = userChoiceRepository;
    }

    public void saveUserChoice(String email, RecommendationOption choice) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserChoice userChoice = new UserChoice();
        userChoice.setUser(user);
        userChoice.setType(choice.getType());
        userChoice.setLabel(choice.getLabel());
        userChoice.setStartTime(choice.getStartTime());
        userChoice.setEndTime(choice.getEndTime());

        // 추천 일정의 상세 정보 저장 (추천X는 null, 추천이면 값 세팅)
        if ("event".equals(choice.getType()) && choice.getData() instanceof List) {
            List<?> dataList = (List<?>) choice.getData();
            Object last = dataList.get(dataList.size() - 1);
            if (last instanceof FormattedTimeSlot slot) {
                userChoice.setEventTitle(slot.getTitle());
                userChoice.setEventDescription(slot.getDescription());
            }
        }

        userChoiceRepository.save(userChoice);
    }
}

