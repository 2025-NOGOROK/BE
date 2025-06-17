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
            if (filteredSlots.isEmpty()) {
                ZonedDateTime fallbackStart = LocalDate.parse(date).atTime(10, 0).atZone(ZoneId.of("Asia/Seoul"));
                ZonedDateTime fallbackEnd = fallbackStart.plusMinutes(60);
                filteredSlots.add(new TimeSlot(
                        new DateTime(fallbackStart.toInstant().toEpochMilli()),
                        new DateTime(fallbackEnd.toInstant().toEpochMilli())
                ));
            }

            // Get recommendations from Flask server
            List<String> recommendations = flaskService.getRecommendations(request);
            List<FormattedTimeSlot> recommendEvents = new ArrayList<>();

            // Combine filtered slots with the recommendations
            for (int i = 0; i < recommendations.size() && i < filteredSlots.size(); i++) {
                TimeSlot slot = filteredSlots.get(i);
                String title = recommendations.get(i);

                // Parse the duration from the title, default to 60 minutes
                int durationMinutes = 60;
                Matcher matcher = Pattern.compile("\\((\\d+)분\\)").matcher(title);
                if (matcher.find()) durationMinutes = Integer.parseInt(matcher.group(1));

                // Create a FormattedTimeSlot for each recommended event
                DateTime start = slot.getStart();
                DateTime end = new DateTime(start.getValue() + durationMinutes * 60 * 1000);

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
                        false, 0, false, false
                );

                // Add event to the list of recommended events
                recommendEvents.add(event);
            }

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
