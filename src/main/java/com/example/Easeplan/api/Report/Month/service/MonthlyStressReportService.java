package com.example.Easeplan.api.Report.Month.service;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;

import com.example.Easeplan.api.Report.Month.dto.MonthlyStressReportResponse;
import com.example.Easeplan.api.Report.Month.dto.SimpleEvent;
import com.example.Easeplan.api.Report.Month.dto.StressDayReport;
import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyStressReportService {

    private final SmartwatchRepository smartwatchRepository;
    private final GoogleCalendarService calendarService;
    private final UserRepository userRepository;

    public MonthlyStressReportResponse getMonthlyStressReport(String email, YearMonth ym) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate first = ym.atDay(1);
        LocalDate last  = ym.atEndOfMonth();

        // 1) 월 범위의 HeartRate 불러와서 일자별 평균 계산
        Map<LocalDate, Double> dailyAvg = new LinkedHashMap<>();
        LocalDate cur = first;
        while (!cur.isAfter(last)) {
            String dayPrefix = cur.toString();

            List<HeartRate> list = smartwatchRepository
                    .findByUserEmailAndStartTimeStartingWith(email, dayPrefix);

            if (list != null && !list.isEmpty()) {
                double avg = list.stream()
                        .map(HeartRate::getAvg)
                        .filter(Objects::nonNull)
                        .mapToDouble(Float::doubleValue)
                        .average()
                        .orElse(Double.NaN);
                if (!Double.isNaN(avg)) dailyAvg.put(cur, avg);
            }
            cur = cur.plusDays(1);
        }

        if (dailyAvg.isEmpty()) {
            // 데이터가 없으면 빈 응답
            return MonthlyStressReportResponse.builder()
                    .year(ym.getYear())
                    .month(ym.getMonthValue())
                    .mostStressful(null)
                    .leastStressful(null)
                    .build();
        }

        // 2) 최댓값/최솟값 날짜 선정 (동률이면 가장 이른 날짜)
        // 최댓값 날짜
        LocalDate maxDay = dailyAvg.entrySet().stream()
                .max(
                        Comparator.<Map.Entry<LocalDate, Double>>comparingDouble((Map.Entry<LocalDate, Double> e) -> e.getValue())
                                .thenComparing((Map.Entry<LocalDate, Double> e) -> e.getKey())
                )
                .get().getKey();

// 최솟값 날짜
        LocalDate minDay = dailyAvg.entrySet().stream()
                .min(
                        Comparator.<Map.Entry<LocalDate, Double>>comparingDouble((Map.Entry<LocalDate, Double> e) -> e.getValue())
                                .thenComparing((Map.Entry<LocalDate, Double> e) -> e.getKey())
                )
                .get().getKey();


        StressDayReport most = buildDayReport(user, maxDay, dailyAvg.get(maxDay));
        StressDayReport least = buildDayReport(user, minDay, dailyAvg.get(minDay));

        return MonthlyStressReportResponse.builder()
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .mostStressful(most)
                .leastStressful(least)
                .build();
    }

    private StressDayReport buildDayReport(User user, LocalDate day, double avg) throws Exception {
        // 해당 날짜의 캘린더 이벤트 조회
        ZoneId KST = ZoneId.of("Asia/Seoul");
        DateTimeFormatter RFC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

        String timeMin = day.atStartOfDay(KST).format(RFC);
        String timeMax = day.plusDays(1).atStartOfDay(KST).format(RFC);

        List<FormattedTimeSlot> events = calendarService.getFormattedEvents(user, "primary", timeMin, timeMax);

        // sourceType 기준으로 나누기
        List<SimpleEvent> shorts = mapByType(events, "short-recommend");
        List<SimpleEvent> longs  = mapByType(events, "long-recommend");
        List<SimpleEvent> emergs = mapByType(events, "emergency"); // 응급 쉼표

        return StressDayReport.builder()
                .date(day)
                .avgStress(round1(avg))
                .shortCount(shorts.size())
                .longCount(longs.size())
                .emergencyCount(emergs.size())
                .shortEvents(shorts)
                .longEvents(longs)
                .emergencyEvents(emergs)
                .build();
    }

    private List<SimpleEvent> mapByType(List<FormattedTimeSlot> events, String type) {
        DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");
        return events.stream()
                .filter(e -> type.equalsIgnoreCase(safe(e.getSourceType())))
                .map(e -> {
                    ZonedDateTime s = ZonedDateTime.parse(e.getStartTime());
                    ZonedDateTime f = ZonedDateTime.parse(e.getEndTime());
                    return SimpleEvent.builder()
                            .title(safe(e.getTitle()))
                            .startTime(s.toLocalTime().format(HHmm))
                            .endTime(f.toLocalTime().format(HHmm))
                            .sourceType(type)
                            .build();
                })
                // ↓ 메서드 레퍼런스 대신 람다 사용
                .sorted(Comparator.comparing(ev -> ev.getStartTime()))
                .collect(Collectors.toList());
    }


    private String safe(String s) { return s == null ? "" : s; }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
