package com.example.Easeplan.api.Calendar.service;

import com.example.Easeplan.api.Calendar.config.GoogleOAuthProperties;
import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.dto.TimeSlot;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;

import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    public Calendar getCalendarService(User user) throws Exception {
        // jwtToken을 통해 구글 Access Token을 얻어옴
        String jwtToken = user.getJwtToken();
        String googleAccessToken = getGoogleAccessTokenFromJwt(jwtToken);

        if (googleAccessToken == null) {
            throw new RuntimeException("구글 액세스 토큰을 얻을 수 없습니다.");
        }

        UserCredentials userCredentials = UserCredentials.newBuilder()
                .setClientId(googleOAuthProperties.getWebClientId())
                .setClientSecret(googleOAuthProperties.getClientSecret())
                .setAccessToken(new AccessToken(googleAccessToken, null))
                .build();

        HttpCredentialsAdapter requestInitializer = new HttpCredentialsAdapter(userCredentials);

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        ).setApplicationName("Easeplan").build();
    }


//    public Calendar getCalendarService(User user) throws Exception {
//        String accessToken = oAuthService.getOrRefreshGoogleAccessToken(user);
//        String refreshToken = user.getGoogleRefreshToken();
//
//        UserCredentials userCredentials = UserCredentials.newBuilder()
//                .setClientId(googleOAuthProperties.getWebClientId())
//                .setClientSecret(googleOAuthProperties.getClientSecret())
//                .setRefreshToken(refreshToken)
//                .setAccessToken(new AccessToken(accessToken, null))
//                .build();
//
//        HttpCredentialsAdapter requestInitializer = new HttpCredentialsAdapter(userCredentials);
//
//        return new Calendar.Builder(
//                GoogleNetHttpTransport.newTrustedTransport(),
//                GsonFactory.getDefaultInstance(),
//                requestInitializer
//        ).setApplicationName("Easeplan").build();
//    }

    public Events getEvents(User user, String calendarId, String timeMinStr, String timeMaxStr) throws Exception {
        Calendar service = getCalendarService(user);
        DateTime timeMin = new DateTime(timeMinStr);
        DateTime timeMax = new DateTime(timeMaxStr);

        return service.events().list(calendarId)
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setMaxResults(100)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
    }

    public List<FormattedTimeSlot> getFormattedEvents(User user, String calendarId, String timeMin, String timeMax) throws Exception {
        Events events = getEvents(user, calendarId, timeMin, timeMax);
        List<FormattedTimeSlot> slots = new ArrayList<>();
        for (Event event : events.getItems()) {
            String title = event.getSummary();
            String description = event.getDescription();
            String startDateTime = event.getStart().getDateTime() != null ? event.getStart().getDateTime().toStringRfc3339() : event.getStart().getDate() != null ? event.getStart().getDate().toString() : null;
            String endDateTime = event.getEnd().getDateTime() != null ? event.getEnd().getDateTime().toStringRfc3339() : event.getEnd().getDate() != null ? event.getEnd().getDate().toString() : null;
            slots.add(new FormattedTimeSlot(title, description, startDateTime, endDateTime));
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

            formattedSlots.add(new FormattedTimeSlot(
                    null,
                    null,
                    start.format(isoFormatter),
                    end.format(isoFormatter)
            ));
        }

        return formattedSlots;
    }

    public Event addEvent(User user, String calendarId, String title, String description, String startDateTime, String endDateTime, boolean serverAlarm, int minutesBeforeAlarm, boolean fixed, boolean userLabel) throws Exception {
        Calendar service = getCalendarService(user);

        Event event = new Event()
                .setSummary(title)
                .setDescription(description)
                .setStart(new EventDateTime().setDateTime(new DateTime(startDateTime)).setTimeZone("Asia/Seoul"))
                .setEnd(new EventDateTime().setDateTime(new DateTime(endDateTime)).setTimeZone("Asia/Seoul"));

        Map<String, String> customProps = new HashMap<>();
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



    // JWT 토큰에서 구글 액세스 토큰을 얻는 메서드
    public String getGoogleAccessTokenFromJwt(String jwtToken) {
        try {
            // JWT 토큰에서 이메일을 추출
            String email = oAuthService.getGoogleUserEmailFromJwt(jwtToken); // 기존의 getGoogleUserEmailFromJwt 메서드를 사용
            // User 객체 가져오기
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            // Google OAuth 액세스 토큰 반환
            return user.getGoogleAccessToken();
        } catch (Exception e) {
            log.error("Error extracting Google Access Token from JWT", e);
            return null;
        }
    }



}
