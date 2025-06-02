package com.example.Easeplan.api.Recommend.Long.repository;

import com.example.Easeplan.api.Calendar.domain.GoogleCalendarInfo;
import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Recommend.Long.dto.RecommendationOption;
import com.example.Easeplan.api.Recommend.Long.dto.UserChoice;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

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
        if ("event".equals(choice.getType())) {
            Object data = choice.getData();

            // 1) data가 FormattedTimeSlot인 경우
            if (data instanceof FormattedTimeSlot slot) {
                userChoice.setEventTitle(slot.getTitle());
                userChoice.setEventDescription(slot.getDescription());
            }
            // 2) data가 List인 경우 (캘린더+추천 일정)
            else if (data instanceof List<?> dataList && !dataList.isEmpty()) {
                Object last = dataList.get(dataList.size() - 1);

                // 2-1) 마지막 요소가 FormattedTimeSlot
                if (last instanceof FormattedTimeSlot slot) {
                    userChoice.setEventTitle(slot.getTitle());
                    userChoice.setEventDescription(slot.getDescription());
                }
                // 2-2) 마지막 요소가 Map (역직렬화된 경우)
                else if (last instanceof Map<?, ?> map) {
                    userChoice.setEventTitle(map.get("title") != null ? map.get("title").toString() : null);
                    userChoice.setEventDescription(map.get("description") != null ? map.get("description").toString() : null);
                }
            }
            // 3) data가 Map인 경우 (단일 객체)
            else if (data instanceof Map<?, ?> map) {
                userChoice.setEventTitle(map.get("title") != null ? map.get("title").toString() : null);
                userChoice.setEventDescription(map.get("description") != null ? map.get("description").toString() : null);
            }
        }

        userChoiceRepository.save(userChoice);
    }




}

