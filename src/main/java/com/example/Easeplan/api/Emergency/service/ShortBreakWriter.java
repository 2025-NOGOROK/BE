package com.example.Easeplan.api.Emergency.service;

import com.example.Easeplan.api.Calendar.dto.TimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.global.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ShortBreakWriter {

    private final GoogleCalendarService calendarService;

    @Transactional
    public void createShortBreakNDays(User user, LocalDate startDateInclusive, List<String> dailyTitles) {
        ZoneId ZONE = ZoneId.of("Asia/Seoul");
        LocalTime DAY_START = LocalTime.of(8, 0);
        LocalTime DAY_END   = LocalTime.of(22, 0);

        for (int i = 0; i < dailyTitles.size(); i++) {
            LocalDate day = startDateInclusive.plusDays(i);
            String title = dailyTitles.get(i);

            int minutes = parseMinutes(title); // "(30분)" → 30, 없으면 60으로 가정

            // 1) 빈 슬롯 조회
            List<TimeSlot> free;
            try {
                free = calendarService.getFreeTimeSlots(user, day);
            } catch (Exception e) {
                // 캘린더 조회 실패 → 폴백
                addEventFallback(user, day, title, minutes);
                continue;
            }

            // 2) 08~22시 사이에서 'minutes' 이상 확보되는 가장 이른 슬롯 찾기
            ZonedDateTime start = null, end = null;
            for (TimeSlot ts : free) {
                ZonedDateTime s = Instant.ofEpochMilli(ts.getStart().getValue()).atZone(ZONE);
                ZonedDateTime e = Instant.ofEpochMilli(ts.getEnd().getValue()).atZone(ZONE);

                // 업무시간으로 클립
                ZonedDateTime minStart = day.atTime(DAY_START).atZone(ZONE);
                ZonedDateTime maxEnd   = day.atTime(DAY_END).atZone(ZONE);
                if (s.isBefore(minStart)) s = minStart;
                if (e.isAfter(maxEnd)) e = maxEnd;

                // 유효 & 길이 체크
                if (e.isAfter(s) && Duration.between(s, e).toMinutes() >= minutes) {
                    start = s;
                    end = s.plusMinutes(minutes);
                    break;
                }
            }

            if (start == null) {
                // 3) 폴백 10:00~(minutes)
                addEventFallback(user, day, title, minutes);
            } else {
                addEvent(user, title, start, end);
            }
        }
    }

    private int parseMinutes(String title) {
        var m = java.util.regex.Pattern.compile("\\((\\d+)분\\)").matcher(title);
        return m.find() ? Integer.parseInt(m.group(1)) : 60;
    }

    private void addEventFallback(User user, LocalDate day, String title, int minutes) {
        ZoneId ZONE = ZoneId.of("Asia/Seoul");
        ZonedDateTime s = day.atTime(10, 0).atZone(ZONE);
        ZonedDateTime e = s.plusMinutes(minutes);
        addEvent(user, title, s, e);
    }

    private void addEvent(User user, String title, ZonedDateTime start, ZonedDateTime end) {
        try {
            String startRfc3339 = start.toOffsetDateTime().toString();
            String endRfc3339   = end.toOffsetDateTime().toString();
            calendarService.addEvent(
                    user, "primary",
                    title,
                    "설문 기반 추천", // 또는 "잠깐 쉬어가요 😊"
                    startRfc3339, endRfc3339,
                    false, 0, false, false, "emergency"
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
