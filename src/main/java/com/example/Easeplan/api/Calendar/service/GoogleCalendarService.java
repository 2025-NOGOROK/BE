package com.example.Easeplan.api.Calendar.service;

import com.example.Easeplan.api.Calendar.config.GoogleOAuthProperties;
import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.dto.TimeSlot;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.exception.GlobalExceptionHandler;
import com.example.Easeplan.global.auth.repository.UserRepository;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;

import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
@Slf4j
@Service
public class GoogleCalendarService {

    private final GoogleOAuthProperties googleOAuthProperties;
    private final GoogleOAuthService oAuthService;
    private final UserRepository userRepository;

    public GoogleCalendarService(GoogleOAuthService oAuthService, UserRepository userRepository, GoogleOAuthProperties googleOAuthProperties) {
        this.oAuthService = oAuthService;
        this.userRepository = userRepository;
        this.googleOAuthProperties = googleOAuthProperties;
    }
    @Transactional
    public Calendar getCalendarService(User authUser) throws Exception {
        // 1) JWT → email 추출 (의도대로 유지)
        final String email = oAuthService.getGoogleUserEmailFromJwt(authUser.getJwtToken());

        // 2) 이메일로 유저 조회
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found by email: " + email));

        // 3) refresh_token 없으면 재연동 요구
        if (u.getGoogleRefreshToken() == null || u.getGoogleRefreshToken().isBlank()) {
            throw new GlobalExceptionHandler.GoogleRelinkRequiredException();
        }

        // 4) 초기 AccessToken (만료시각은 UTC로)
        AccessToken initial = null;
        if (u.getGoogleAccessToken() != null && u.getGoogleAccessTokenExpiresAt() != null) {
            Date expiryUtc = Date.from(u.getGoogleAccessTokenExpiresAt().atZone(ZoneId.of("UTC")).toInstant());
            initial = new AccessToken(u.getGoogleAccessToken(), expiryUtc);
        }

        // 5) UserCredentials
        UserCredentials creds = UserCredentials.newBuilder()
                .setClientId(googleOAuthProperties.getWebClientId())
                .setClientSecret(googleOAuthProperties.getClientSecret())
                .setRefreshToken(u.getGoogleRefreshToken())
                .setAccessToken(initial)
                .build();

        // 6) 만료 시 자동 갱신 (라이브러리에게 맡김)
        try {
            creds.refreshIfExpired();
        } catch (IOException e) {
            throw new GlobalExceptionHandler.GoogleRelinkRequiredException(e);
        }

        AccessToken current = creds.getAccessToken();
        if (current == null || current.getTokenValue() == null) {
            throw new GlobalExceptionHandler.GoogleRelinkRequiredException();
        }

        // 7) 최신 토큰/만료시각 DB 반영 (UTC로 저장)
        LocalDateTime newExpUtc = current.getExpirationTime() == null
                ? null
                : LocalDateTime.ofInstant(current.getExpirationTime().toInstant(), ZoneId.of("UTC"));
        u.setGoogleAccessToken(current.getTokenValue());
        u.setGoogleAccessTokenExpiresAt(newExpUtc);
        userRepository.save(u);

        // 8) Calendar 클라이언트
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(creds)
        ).setApplicationName("Easeplan").build();
    }

    @FunctionalInterface
    interface Call<T> { T run() throws Exception; }

    private <T> T withRetry401(User user, Call<T> c) throws Exception {
        try {
            return c.run();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            if (e.getStatusCode() == 401) {
                // 내부 갱신 후 1회 재시도
                getCalendarService(user);
                return c.run();
            }
            throw e;
        }
    }





    public Events getEvents(User user, String calendarId, String timeMinStr, String timeMaxStr) throws Exception {
        Calendar service = getCalendarService(user);
        DateTime timeMin = new DateTime(timeMinStr);
        DateTime timeMax = new DateTime(timeMaxStr);

        return withRetry401(user, () ->
                service.events().list(calendarId)
                        .setTimeMin(timeMin)
                        .setTimeMax(timeMax)
                        .setMaxResults(100)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute()
        );
    }


    public List<FormattedTimeSlot> getFormattedEvents(User user, String calendarId, String timeMin, String timeMax) throws Exception {
        Events events = getEvents(user, calendarId, timeMin, timeMax);
        List<FormattedTimeSlot> slots = new ArrayList<>();

        for (Event event : events.getItems()) {
            String title = event.getSummary();
            String description = event.getDescription();
            String startDateTime = event.getStart().getDateTime() != null
                    ? event.getStart().getDateTime().toStringRfc3339()
                    : event.getStart().getDate() != null
                    ? event.getStart().getDate().toString()
                    : null;
            String endDateTime = event.getEnd().getDateTime() != null
                    ? event.getEnd().getDateTime().toStringRfc3339()
                    : event.getEnd().getDate() != null
                    ? event.getEnd().getDate().toString()
                    : null;

            // sourceType을 동적으로 설정
            String sourceType = "calendar"; // 기본값

            Map<String, String> props = event.getExtendedProperties() != null
                    ? event.getExtendedProperties().getPrivate()
                    : null;

            if ("설문 기반 추천".equals(description)) {
                sourceType = "short-recommend";
            } else if (props != null && "long-recommend".equals(props.get("sourceType"))) {
                sourceType = "long-recommend"; // ✅ 이 조건에서 잡힘
            }else {
                sourceType = "calendar"; // 🔚 나머지는 모두 calendar
            }

            // FormattedTimeSlot 생성 시 sourceType 설정
            slots.add(new FormattedTimeSlot(
                    title,
                    description,
                    startDateTime,
                    endDateTime,
                    sourceType  // 동적으로 설정된 sourceType
            ));

            // 디버깅 로그
            log.debug("Event: {} | Description: {} | SourceType: {}", title, description, sourceType);
        }

        return slots;
    }



    public List<TimeSlot> getFreeTimeSlots(User user, LocalDate date) throws Exception {
        Calendar service = getCalendarService(user);
        DateTime timeMin = new DateTime(date.toString() + "T00:00:00.000+09:00");
        DateTime timeMax = new DateTime(date.plusDays(1).toString() + "T00:00:00.000+09:00");

        FreeBusyRequest request = new FreeBusyRequest()
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setTimeZone("Asia/Seoul")
                .setItems(List.of(new FreeBusyRequestItem().setId("primary")));

        FreeBusyResponse response = service.freebusy().query(request).execute();

        List<FreeBusyCalendar> calendars = response.getCalendars().values().stream().toList();
        List<TimeSlot> busySlots = calendars.get(0).getBusy().stream()
                .map(busy -> new TimeSlot(busy.getStart(), busy.getEnd()))
                .sorted(Comparator.comparingLong(slot -> slot.getStart().getValue()))
                .toList();

        return calculateFreeSlots(busySlots, timeMin, timeMax);
    }

    private List<TimeSlot> calculateFreeSlots(List<TimeSlot> busySlots, DateTime timeMin, DateTime timeMax) {
        List<TimeSlot> freeSlots = new ArrayList<>();
        DateTime currentStart = timeMin;

        for (TimeSlot busy : busySlots) {
            if (busy.getStart().getValue() > currentStart.getValue()) {
                freeSlots.add(new TimeSlot(currentStart, busy.getStart()));
            }
            if (busy.getEnd().getValue() > currentStart.getValue()) {
                currentStart = busy.getEnd();
            }
        }

        if (currentStart.getValue() < timeMax.getValue()) {
            freeSlots.add(new TimeSlot(currentStart, timeMax));
        }

        return freeSlots.stream()
                .filter(slot -> getDurationMinutes(slot) >= 30)
                .toList();
    }

    private long getDurationMinutes(TimeSlot slot) {
        long diff = slot.getEnd().getValue() - slot.getStart().getValue();
        return TimeUnit.MILLISECONDS.toMinutes(diff);
    }

    public List<FormattedTimeSlot> getFormattedFreeTimeSlots(User user, LocalDate date) throws Exception {
        List<TimeSlot> freeSlots = getFreeTimeSlots(user, date);
        List<FormattedTimeSlot> formattedSlots = new ArrayList<>();
        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        for (TimeSlot slot : freeSlots) {
            ZonedDateTime start = Instant.ofEpochMilli(slot.getStart().getValue()).atZone(seoulZone);
            ZonedDateTime end = Instant.ofEpochMilli(slot.getEnd().getValue()).atZone(seoulZone);

            // Use empty strings for title and description if they are null
            formattedSlots.add(new FormattedTimeSlot(
                    "",  // Empty string for title
                    "",  // Empty string for description
                    start.format(isoFormatter),
                    end.format(isoFormatter),
                    "long-recommend"
            ));
        }

        return formattedSlots;
    }


    public Event addEvent(User user, String calendarId, String title, String description, String startDateTime, String endDateTime, boolean serverAlarm, int minutesBeforeAlarm, boolean fixed, boolean userLabel, String sourceType ) throws Exception {
        Calendar service = getCalendarService(user);

        Event event = new Event()
                .setSummary(title)
                .setDescription(description)
                .setStart(new EventDateTime().setDateTime(new DateTime(startDateTime)).setTimeZone("Asia/Seoul"))
                .setEnd(new EventDateTime().setDateTime(new DateTime(endDateTime)).setTimeZone("Asia/Seoul"));

        Map<String, String> customProps = new HashMap<>();
        // ✅ 자동 추천만 sourceType 저장
        if (sourceType != null && !sourceType.equals("calendar")) {
            customProps.put("sourceType", sourceType);
        }
        customProps.put("serverAlarm", String.valueOf(serverAlarm));
        customProps.put("minutesBeforeAlarm", String.valueOf(minutesBeforeAlarm));
        customProps.put("fixed", String.valueOf(fixed));
        customProps.put("userLabel", String.valueOf(userLabel));
        event.setExtendedProperties(new Event.ExtendedProperties().setPrivate(customProps));

        if (serverAlarm) {
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Collections.singletonList(
                            new EventReminder().setMethod("popup").setMinutes(minutesBeforeAlarm)
                    ));
            event.setReminders(reminders);
        }

        return service.events().insert(calendarId, event).execute();
    }

    public void deleteEvent(User user, String calendarId, String eventId) throws Exception {
        Calendar service = getCalendarService(user);
        service.events().delete(calendarId, eventId).execute();
    }

}
