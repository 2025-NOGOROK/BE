package com.example.Easeplan.api.Recommend.Short.controller;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.dto.TimeSlot;
import com.example.Easeplan.api.ShortFlask.service.FlaskRecommendService;
import com.example.Easeplan.api.Survey.domain.UserSurvey;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.global.auth.domain.User;
import com.google.api.client.util.DateTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.Easeplan.global.auth.repository.UserRepository;
import com.example.Easeplan.api.Survey.service.UserSurveyService;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Tag(name = "짧은 추천", description = "짧은 쉼표")
@RestController
public class ShortController {

    private final UserRepository userRepository;
    private final UserSurveyService surveyService;
    private final GoogleCalendarService calendarService;
    private final FlaskRecommendService flaskService;

    private final Map<String, Set<String>> recommendationHistoryMap = new HashMap<>();

    @Autowired
    public ShortController(UserRepository userRepository,
                           UserSurveyService surveyService,
                           GoogleCalendarService calendarService,
                           FlaskRecommendService flaskService) {
        this.userRepository = userRepository;
        this.surveyService = surveyService;
        this.calendarService = calendarService;
        this.flaskService = flaskService;
    }

    @PostMapping("/api/short-recommend")
    public ResponseEntity<List<FormattedTimeSlot>> createAndSaveShortRecommendation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String date
    ) {
        try {
            if (userDetails == null) return ResponseEntity.status(401).build();

            // Get the user based on email
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

            // Get the user's survey details
            UserSurvey survey = surveyService.getSurveyByUser(user);
            UserSurveyRequest request = UserSurveyRequest.fromEntity(survey);

            // Get the start and end of the day in ZonedDateTime format
            ZonedDateTime startOfDay = LocalDate.parse(date).atStartOfDay(ZoneId.of("Asia/Seoul"));
            ZonedDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            // Get events from Google Calendar for the user
            List<FormattedTimeSlot> events = calendarService.getFormattedEvents(
                    user, "primary",
                    startOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    endOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );

            // No need to call setSourceType, sourceType is set during construction
            List<FormattedTimeSlot> allEvents = new ArrayList<>();
            for (FormattedTimeSlot event : events) {
                allEvents.add(new FormattedTimeSlot(
                        event.getTitle(),
                        event.getDescription(),
                        event.getStartDateTime(),
                        event.getEndDateTime(),
                        "calendar"
                ));
            }

            // Get free time slots
            List<TimeSlot> freeSlots = calendarService.getFreeTimeSlots(user, LocalDate.parse(date));
            List<TimeSlot> filteredSlots = freeSlots.stream()
                    .filter(slot -> {
                        Instant startInstant = Instant.ofEpochMilli(slot.getStart().getValue());
                        ZonedDateTime startKST = startInstant.atZone(ZoneId.of("Asia/Seoul"));
                        return startKST.toLocalTime().isAfter(LocalTime.of(5, 59));
                    }).collect(Collectors.toList());

            // If no free slots are found, add a fallback slot
            while (filteredSlots.size() < 2) {
                ZonedDateTime fallbackStart = LocalDate.parse(date)
                        .atTime(10 + filteredSlots.size(), 0)
                        .atZone(ZoneId.of("Asia/Seoul"));
                ZonedDateTime fallbackEnd = fallbackStart.plusMinutes(60);
                filteredSlots.add(new TimeSlot(
                        new DateTime(fallbackStart.toInstant().toEpochMilli()),
                        new DateTime(fallbackEnd.toInstant().toEpochMilli())
                ));
            }

            // Get recommendations from Flask server
            // 추천 받아오기 + 중복 제거
            List<String> recommendations = flaskService.getRecommendations(request);
            Collections.shuffle(recommendations); // 순서 섞기

            String userEmail = user.getEmail();  // user.getId() 대신 email 기준
            Set<String> history = recommendationHistoryMap.getOrDefault(userEmail, new HashSet<>());

            List<String> filteredRecommendations = recommendations.stream()
                    .filter(r -> !history.contains(r))
                    .limit(3) // 최대 2개만
                    .collect(Collectors.toList());

            List<FormattedTimeSlot> recommendEvents = new ArrayList<>();

            int count = Math.min(2, Math.min(filteredRecommendations.size(), filteredSlots.size()));


            // ✅ 2시간 간격 기준 시작 시간 설정
            ZonedDateTime baseStart = Instant.ofEpochMilli(filteredSlots.get(0).getStart().getValue())
                    .atZone(ZoneId.of("Asia/Seoul"));

            // Combine filtered slots with the recommendations
            for (int i = 0; i < count; i++) {
                String title = filteredRecommendations.get(i);

                // Parse the duration from the title, default to 60 minutes
                int durationMinutes = 60;
                Matcher matcher = Pattern.compile("\\((\\d+)분\\)").matcher(title);
                if (matcher.find()) durationMinutes = Integer.parseInt(matcher.group(1));

                ZonedDateTime startZoned = baseStart.plusHours(i * 2); // 2시간 간격
                ZonedDateTime endZoned = startZoned.plusMinutes(durationMinutes);

                // Create a FormattedTimeSlot for each recommended event
                DateTime start = new DateTime(startZoned.toInstant().toEpochMilli());
                DateTime end = new DateTime(endZoned.toInstant().toEpochMilli());

                FormattedTimeSlot event = new FormattedTimeSlot(
                        title, "설문 기반 추천",
                        start.toStringRfc3339(),
                        end.toStringRfc3339(),
                        "short-recommend"  // Set sourceType as "short-recommend"
                );

                // Add event to Google Calendar
                calendarService.addEvent(
                        user, "primary",
                        event.getTitle(),
                        event.getDescription(),
                        start.toStringRfc3339(),
                        end.toStringRfc3339(),
                        false, 0, false, false, "short-recommend"
                );

                // Add event to the list of recommended events
                recommendEvents.add(event);
                history.add(title); // 중복 방지용 저장
            }

            recommendationHistoryMap.put(userEmail, history); // 히스토리 저장

            // Combine the calendar events and recommended events
            allEvents.addAll(recommendEvents);

            // Return the combined list of events
            return ResponseEntity.ok(allEvents);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
