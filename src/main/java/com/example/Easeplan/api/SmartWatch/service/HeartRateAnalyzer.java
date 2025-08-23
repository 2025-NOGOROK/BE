package com.example.Easeplan.api.SmartWatch.service;

import com.example.Easeplan.api.SmartWatch.domain.HeartRate;

import java.time.*;
import java.util.*;
import java.util.stream.*;

public class HeartRateAnalyzer {

    private static LocalDate toLocalDateKST(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
    }

    /** 날짜별 평균 stressEma */
    public static Map<LocalDate, Double> getDateAvgStress(List<HeartRate> heartRates) {
        return heartRates.stream()
                .filter(hr -> hr.getTimestamp() != null && hr.getStressEma() != null)
                .collect(Collectors.groupingBy(
                        hr -> toLocalDateKST(hr.getTimestamp()),
                        Collectors.averagingDouble(HeartRate::getStressEma)
                ));
    }

    /** 요일별 평균 stressEma (월~일) */
    public static Map<String, Double> getDayOfWeekAvgStress(List<HeartRate> heartRates) {
        return heartRates.stream()
                .filter(hr -> hr.getTimestamp() != null && hr.getStressEma() != null)
                .collect(Collectors.groupingBy(
                        hr -> getKorDayOfWeek(toLocalDateKST(hr.getTimestamp()).getDayOfWeek()),
                        Collectors.averagingDouble(HeartRate::getStressEma)
                ));
    }

    private static String getKorDayOfWeek(DayOfWeek dow) {
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
