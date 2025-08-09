package com.example.Easeplan.api.Report.Month.service;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.api.Report.Month.dto.MonthlyReportResponse;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private final GoogleCalendarService calendarService;
    private final UserRepository userRepository;

    public MonthlyReportResponse getMonthlyPauseReport(String email, YearMonth ym) throws Exception { // ← 타입 수정
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ZoneId KST = ZoneId.of("Asia/Seoul");
        DateTimeFormatter RFC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

        ZonedDateTime startOfMonth = ym.atDay(1).atStartOfDay(KST);
        ZonedDateTime endOfMonth   = ym.atEndOfMonth().atTime(23,59,59).atZone(KST);

        YearMonth prev = ym.minusMonths(1);
        ZonedDateTime prevStart = prev.atDay(1).atStartOfDay(KST);
        ZonedDateTime prevEnd   = prev.atEndOfMonth().atTime(23,59,59).atZone(KST);

        List<FormattedTimeSlot> thisMonth = calendarService.getFormattedEvents(
                user, "primary", startOfMonth.format(RFC), endOfMonth.format(RFC));
        List<FormattedTimeSlot> prevMonth = calendarService.getFormattedEvents(
                user, "primary", prevStart.format(RFC), prevEnd.format(RFC));

        int shortThis = countBySourceType(thisMonth, true);
        int longThis  = countBySourceType(thisMonth, false);
        int shortPrev = countBySourceType(prevMonth, true);
        int longPrev  = countBySourceType(prevMonth, false);

        return MonthlyReportResponse.builder()   // ← 여기!
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .shortCount(shortThis)
                .longCount(longThis)
                .shortDiffFromPrev(shortThis - shortPrev)
                .longDiffFromPrev(longThis - longPrev)
                .build();
    }

    private int countBySourceType(List<FormattedTimeSlot> events, boolean isShort) {
        int cnt = 0;
        for (FormattedTimeSlot e : events) {
            String src = safe(e.getSourceType());
            String desc = safe(e.getDescription());

            boolean isShortHit = "short-recommend".equalsIgnoreCase(src)
                    || "설문 기반 추천".equals(desc); // 과거 보정
            boolean isLongHit  = "long-recommend".equalsIgnoreCase(src);

            if (isShort && isShortHit) cnt++;
            if (!isShort && isLongHit) cnt++;
        }
        return cnt;
    }
    private String safe(String s) { return s == null ? "" : s.trim(); }
}
