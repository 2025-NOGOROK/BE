package com.example.Easeplan.api.Calendar.service;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.dto.TimeSlot;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;

import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleCalendarService {
    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    private final GoogleOAuthService oAuthService;
    private final UserRepository userRepository; // User 엔티티를 직접 수정할 필요는 없지만, 주입되어 있다면 유지

    public GoogleCalendarService(GoogleOAuthService oAuthService, UserRepository userRepository) {
        this.oAuthService = oAuthService;
        this.userRepository = userRepository;
    }

    /**
     * User 객체를 기반으로 구글 캘린더 서비스를 생성합니다.
     * 이 메서드는 oAuthService를 통해 항상 유효한 access_token을 가져와 사용합니다.
     * @param user 현재 로그인한 사용자 엔티티 (구글 토큰 정보를 포함)
     * @return 구글 캘린더 API 서비스 객체
     * @throws Exception HTTP 전송 또는 JSON 파싱 중 오류 발생 시
     */
    public Calendar getCalendarService(User user) throws Exception {
        // **[핵심]** oAuthService를 통해 항상 최신이고 유효한 access_token을 가져옵니다.
        String accessToken = oAuthService.getOrRefreshGoogleAccessToken(user);
        String refreshToken = user.getGoogleRefreshToken(); // User 객체에서 refresh token 가져오기

        // GoogleClientSecrets는 한 번만 생성하여 재사용하는 것이 효율적입니다.
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setWeb(new GoogleClientSecrets.Details()
                        .setClientId(clientId)
                        .setClientSecret(clientSecret));

        // GoogleCredential 생성: access_token과 refresh_token을 설정
        // 이 credential 객체는 필요시 자체적으로 refresh()를 호출할 수 있지만,
        // 우리는 oAuthService에서 미리 갱신된 토큰을 받아오므로 여기서는 주로 설정 역할입니다.
        UserCredentials userCredentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .setAccessToken(new AccessToken(accessToken, null)) // 만료일 관리 필요시 두 번째 파라미터에 만료일 전달
                .build();

        HttpCredentialsAdapter requestInitializer = new HttpCredentialsAdapter(userCredentials);

        Calendar service = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        ).setApplicationName("Easeplan").build();
        return service; // ✅ 반드시 return 추가!
    }

    // --- (이하 모든 캘린더 API 호출 메서드는 user 객체를 첫 번째 인자로 받도록 변경) ---

    public Events getEvents(User user, String calendarId, String timeMinStr, String timeMaxStr) throws Exception {
        Calendar service = getCalendarService(user); // User 객체 전달
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

    public List<FormattedTimeSlot> getFormattedEvents(
            User user,
            String calendarId,
            String timeMin,
            String timeMax
    ) throws Exception {
        Events events = getEvents(user, calendarId, timeMin, timeMax);
        List<FormattedTimeSlot> slots = new ArrayList<>();
        for (Event event : events.getItems()) {
            String title = event.getSummary();
            String description = event.getDescription();
            String startDateTime;
            if (event.getStart().getDateTime() != null) {
                startDateTime = event.getStart().getDateTime().toStringRfc3339();
            } else if (event.getStart().getDate() != null) {
                startDateTime = event.getStart().getDate().toString();
            } else {
                startDateTime = null;
            }
            String endDateTime;
            if (event.getEnd().getDateTime() != null) {
                endDateTime = event.getEnd().getDateTime().toStringRfc3339();
            } else if (event.getEnd().getDate() != null) {
                endDateTime = event.getEnd().getDate().toString();
            } else {
                endDateTime = null;
            }
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

    public Event addEvent(
            User user,
            String calendarId,
            String title,
            String description,
            String startDateTime,
            String endDateTime,
            boolean serverAlarm,
            int minutesBeforeAlarm,
            boolean fixed,
            boolean userLabel
    ) throws Exception {
        Calendar service = getCalendarService(user);

        Event event = new Event()
                .setSummary(title)
                .setDescription(description)
                .setStart(new EventDateTime()
                        .setDateTime(new DateTime(startDateTime))
                        .setTimeZone("Asia/Seoul"))
                .setEnd(new EventDateTime()
                        .setDateTime(new DateTime(endDateTime))
                        .setTimeZone("Asia/Seoul"));

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

    public void deleteEvent(
            User user,
            String calendarId,
            String eventId
    ) throws Exception {
        Calendar service = getCalendarService(user);
        service.events().delete(calendarId, eventId).execute();
    }
}