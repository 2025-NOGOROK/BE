package com.example.Easeplan.api.Calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor // ✅ 모든 필드 초기화 생성자 생성
public class FormattedTimeSlot {
    private final String title;
    private final String description;
    private final String startTime;
    private final String endTime;

    // 2개 파라미터 생성자 제거 ❌
    // public FormattedTimeSlot(String startTime, String endTime) { ... }

    // Google Calendar용 ISO 형식 변환 메서드 유지
    public String getStartDateTime() {
        return startTime; // 변환 없이 그대로 반환
    }

    public String getEndDateTime() {
        return endTime;
    }

//    private String toIsoDateTime(String time) {
//        String hour = time.replace("시", "").trim();
//        return LocalDate.now() + "T" + String.format("%02d:00:00", Integer.parseInt(hour)) + "+09:00";
//    }
private String toIsoDateTime(String time) {
    // 날짜만 들어온 경우 (올데이 이벤트)
    if (time.matches("\\d{4}-\\d{2}-\\d{2}")) {
        // 이미 ISO-8601 날짜 형식이므로 그대로 반환하거나, 필요시 T00:00:00+09:00 등 붙이기
        return time + "T00:00:00+09:00";
    }
    // "10시" 등 시간만 들어온 경우
    String hour = time.replace("시", "").trim();
    return LocalDate.now() + "T" + String.format("%02d:00:00", Integer.parseInt(hour)) + "+09:00";
}

}
