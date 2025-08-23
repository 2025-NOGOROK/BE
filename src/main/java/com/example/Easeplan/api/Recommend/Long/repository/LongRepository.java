package com.example.Easeplan.api.Recommend.Long.repository;

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

        String eventTitle = null;
        String eventDesc  = null;

        if ("event".equals(choice.getType())) {
            Object data = choice.getData();

            if (data instanceof FormattedTimeSlot slot) {
                eventTitle = slot.getTitle();
                eventDesc  = slot.getDescription();
            } else if (data instanceof List<?> dataList && !dataList.isEmpty()) {
                Object last = dataList.get(dataList.size() - 1);
                if (last instanceof FormattedTimeSlot slot) {
                    eventTitle = slot.getTitle();
                    eventDesc  = slot.getDescription();
                } else if (last instanceof Map<?, ?> map) {
                    eventTitle = map.get("title") != null ? map.get("title").toString() : null;
                    eventDesc  = map.get("description") != null ? map.get("description").toString() : null;
                }
            } else if (data instanceof Map<?, ?> map) {
                eventTitle = map.get("title") != null ? map.get("title").toString() : null;
                eventDesc  = map.get("description") != null ? map.get("description").toString() : null;
            }
        }

        UserChoice userChoice = UserChoice.builder()
                .user(user)
                .type(choice.getType())
                .label(choice.getLabel())
                .startTime(choice.getStartTime())
                .endTime(choice.getEndTime())
                .eventTitle(eventTitle)
                .eventDescription(eventDesc)
                .build();

        userChoiceRepository.save(userChoice);
    }
}
