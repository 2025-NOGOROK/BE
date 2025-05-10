package com.example.Easeplan.api.Calendar.service;

import com.example.Easeplan.api.Calendar.dto.TimeSlot;
import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleCalendarService {

    public Calendar getCalendarService(String accessToken) throws Exception {
        AccessToken token = new AccessToken(accessToken, null); // 만료시간 null 가능
        GoogleCredentials credentials = GoogleCredentials.create(token);
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("YOUR_APP_NAME").build();
    }

    // 1. 일정 전체 조회 (기존 기능)
// 1. 일정 전체 조회 (날짜 범위 필터링 추가)
    public Events getEvents(String accessToken, String calendarId, String timeMinStr, String timeMaxStr) throws Exception {
        Calendar service = getCalendarService(accessToken);

        // 인코딩하지 않고, 문자열 그대로 사용
        DateTime timeMin = new DateTime(timeMinStr); // 또는 DateTime.parseRfc3339(timeMinStr)
        DateTime timeMax = new DateTime(timeMaxStr);

        // 3. API 호출
        return service.events().list(calendarId)
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setMaxResults(100)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
    }

    // 2. 빈 시간대(밀리초) 조회 (기존 기능)
    public List<TimeSlot> getFreeTimeSlots(String accessToken, LocalDate date) throws Exception {
        Calendar service = getCalendarService(accessToken);

        // 시간 범위 설정 (하루 전체)
        DateTime timeMin = new DateTime(date.toString() + "T00:00:00.000+09:00");
        DateTime timeMax = new DateTime(date.plusDays(1).toString() + "T00:00:00.000+09:00");

        // FreeBusy 요청 생성
        FreeBusyRequest request = new FreeBusyRequest()
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setTimeZone("Asia/Seoul")
                .setItems(List.of(new FreeBusyRequestItem().setId("primary")));

        // API 호출
        FreeBusyResponse response = service.freebusy().query(request).execute();

        // busy 시간대 추출
        List<FreeBusyCalendar> calendars = response.getCalendars().values().stream().toList();
        List<TimeSlot> busySlots = calendars.get(0).getBusy().stream()
                .map(busy -> new TimeSlot(busy.getStart(), busy.getEnd()))
                .sorted(Comparator.comparingLong(slot -> slot.getStart().getValue()))
                .toList();

        // 빈 시간 계산
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

        // 30분 이상만 필터링
        return freeSlots.stream()
                .filter(slot -> getDurationMinutes(slot) >= 30)
                .toList();
    }

    private long getDurationMinutes(TimeSlot slot) {
        long diff = slot.getEnd().getValue() - slot.getStart().getValue();
        return TimeUnit.MILLISECONDS.toMinutes(diff);
    }

    // 3. 빈 시간대 "0시 ~ 19시" 형식 반환 (신규 기능)
    public List<FormattedTimeSlot> getFormattedFreeTimeSlots(String accessToken, LocalDate date) throws Exception {
        List<TimeSlot> freeSlots = getFreeTimeSlots(accessToken, date);
        List<FormattedTimeSlot> formattedSlots = new ArrayList<>();
        ZoneId seoulZone = ZoneId.of("Asia/Seoul");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H시");

        for (TimeSlot slot : freeSlots) {
            ZonedDateTime start = Instant.ofEpochMilli(slot.getStart().getValue()).atZone(seoulZone);
            ZonedDateTime end = Instant.ofEpochMilli(slot.getEnd().getValue()).atZone(seoulZone);

            // endTime이 0시(00:00)이면서, startTime이 0보다 크면 "24시"로 표시
            String endDisplay = (end.getHour() == 0 && start.getHour() > 0) ? "24시" : end.format(formatter);
            formattedSlots.add(new FormattedTimeSlot(
                    start.format(formatter),
                    endDisplay
            ));
        }
        return formattedSlots;
    }

    // 일정 추가
    public Event addEvent(
            String accessToken,
            String calendarId,
            String title,
            String description,
            String startDateTime,
            String endDateTime,
            boolean serverAlarm,
            int minutesBeforeAlarm, // <-- 추가!
            boolean aiRecommend,
            boolean fixed,
            boolean userLabel
    ) throws Exception {
        Calendar service = getCalendarService(accessToken);

        Event event = new Event()
                .setSummary(title)
                .setDescription(description)
                .setStart(new EventDateTime()
                        .setDateTime(new DateTime(startDateTime))
                        .setTimeZone("Asia/Seoul"))
                .setEnd(new EventDateTime()
                        .setDateTime(new DateTime(endDateTime))
                        .setTimeZone("Asia/Seoul"));

        // Custom 필드 저장
        Map<String, String> customProps = new HashMap<>();
        customProps.put("serverAlarm", String.valueOf(serverAlarm));
        customProps.put("minutesBeforeAlarm", String.valueOf(minutesBeforeAlarm));
        customProps.put("aiRecommend", String.valueOf(aiRecommend));
        customProps.put("fixed", String.valueOf(fixed));
        customProps.put("userLabel", String.valueOf(userLabel));
        event.setExtendedProperties(new Event.ExtendedProperties().setPrivate(customProps));

        // 서버 알림 설정 (사용자 입력값 반영)
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


    // 일정 삭제
    public void deleteEvent(String accessToken, String calendarId, String eventId) throws Exception {
        Calendar service = getCalendarService(accessToken);
        service.events().delete(calendarId, eventId).execute();
    }
}
