package com.example.Easeplan.api.SmartWatch.service;

import com.example.Easeplan.api.SmartWatch.domain.HeartRate;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

public class HeartRateAnalyzer {

    // 날짜+시간이 "2025-05-22T17:00:00" 형식이라고 가정
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static Map<LocalDate, Double> getDateAvgStress(List<HeartRate> heartRates) {
        return heartRates.stream()
                .collect(Collectors.groupingBy(
                        hr -> LocalDateTime.parse(hr.getStartTime(), formatter).toLocalDate(),
                        Collectors.averagingDouble(hr -> hr.getStress() == null ? 0 : hr.getStress())
                ));
    }

    public static Map<String, Double> getDayOfWeekAvgStress(List<HeartRate> heartRates) {
        return heartRates.stream()
                .collect(Collectors.groupingBy(
                        hr -> {
                            LocalDate date = LocalDateTime.parse(hr.getStartTime(), formatter).toLocalDate();
                            return getKorDayOfWeek(date.getDayOfWeek());
                        },
                        Collectors.averagingDouble(hr -> hr.getStress() == null ? 0 : hr.getStress())
                ));
    }

    private static String getKorDayOfWeek(java.time.DayOfWeek dow) {
        switch (dow) {
            case MONDAY: return "월";
            case TUESDAY: return "화";
            case WEDNESDAY: return "수";
            case THURSDAY: return "목";
            case FRIDAY: return "금";
            case SATURDAY: return "토";
            case SUNDAY: return "일";
            default: return "";
        }
    }
}
