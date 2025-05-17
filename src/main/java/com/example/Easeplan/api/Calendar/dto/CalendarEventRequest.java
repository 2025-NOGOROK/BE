package com.example.Easeplan.api.Calendar.dto;

public class CalendarEventRequest {

    public String calendarId = "primary";
    public String title;
    public String description;
    public String startDateTime; // ISO 8601 (예: 2025-05-10T09:00:00+09:00)
    public String endDateTime;   // ISO 8601
    public boolean serverAlarm;  // 서버 알림 설정 여부
    public int minutesBeforeAlarm; //몇 분 전에 할지
    public boolean fixed;        // 일정 고정 여부
    public boolean userLabel;    // 사용자가 직접 입력한 일정 여부
}
