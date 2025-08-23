package com.example.Easeplan.api.Report.Week.service;

import com.example.Easeplan.api.HaruRecord.domain.DailyEvaluation;
import com.example.Easeplan.api.HaruRecord.repository.DailyEvaluationRepository;
import com.example.Easeplan.api.Report.Week.dto.*;
import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeeklyService {

    private final DailyEvaluationRepository dailyEvaluationRepository;
    private final SmartwatchRepository smartwatchRepository;

    // 생성자에서 두 repository 모두 주입
    public WeeklyService(DailyEvaluationRepository dailyEvaluationRepository,
                         SmartwatchRepository smartwatchRepository) {
        this.dailyEvaluationRepository = dailyEvaluationRepository;
        this.smartwatchRepository = smartwatchRepository;
    }

    public WeeklyEmotionFatigueResponse getCurrentWeekEmotionFatigue(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        List<DailyEvaluation> evaluations = dailyEvaluationRepository.findAllByUserAndDateBetween(user, startOfWeek, endOfWeek);
        Map<DayOfWeek, DailyEvaluation> evalMap = evaluations.stream()
                .collect(Collectors.toMap(e -> e.getDate().getDayOfWeek(), e -> e));

        List<DayEmotionFatigueDto> days = Arrays.stream(DayOfWeek.values())
                .map(dow -> {
                    DailyEvaluation eval = evalMap.get(dow);
                    return new DayEmotionFatigueDto(
                            dow.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                            eval != null ? eval.getEmotion().getDescription() : null,
                            eval != null ? eval.getFatigueLevel().getDescription() : null
                    );
                })
                .collect(Collectors.toList());

        return new WeeklyEmotionFatigueResponse(days);
    }



    public WeeklyWeatherResponse getCurrentWeekWeather(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        List<DailyEvaluation> evaluations = dailyEvaluationRepository.findAllByUserAndDateBetween(user, startOfWeek, endOfWeek);
        Map<DayOfWeek, DailyEvaluation> evalMap = evaluations.stream()
                .collect(Collectors.toMap(e -> e.getDate().getDayOfWeek(), e -> e));

        List<DayWeatherDto> days = Arrays.stream(DayOfWeek.values())
                .map(dow -> {
                    DailyEvaluation eval = evalMap.get(dow);
                    return new DayWeatherDto(
                            dow.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                            eval != null ? eval.getWeather().getDescription() : null
                    );
                })
                .collect(Collectors.toList());

        return new WeeklyWeatherResponse(days);
    }




    // 생성자 주입
    public WeeklyStressResponse getCurrentWeekStress(User user) {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(KST);
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        long weekStartMs = startOfWeek.atStartOfDay(KST).toInstant().toEpochMilli();
        long weekEndExMs = endOfWeek.plusDays(1).atStartOfDay(KST).toInstant().toEpochMilli(); // exclusive

        // 주간 전체 샘플 조회 (포함-포함 메서드이므로 end-1 전달)
        List<HeartRate> heartRates = smartwatchRepository
                .findByUserEmailAndTimestampBetween(user.getEmail(), weekStartMs, weekEndExMs - 1);

        // 요일별 그룹핑 (timestamp → KST → DayOfWeek)
        Map<DayOfWeek, List<HeartRate>> byDay = heartRates.stream()
                .filter(hr -> hr.getTimestamp() != null) // 방어코드
                .collect(Collectors.groupingBy(hr ->
                        Instant.ofEpochMilli(hr.getTimestamp()).atZone(KST).getDayOfWeek()
                ));

        // 월~일 순서로 평균 stressEma 계산
        List<DayStressDto> days = Arrays.stream(DayOfWeek.values())
                .map(dow -> {
                    List<HeartRate> list = byDay.getOrDefault(dow, Collections.emptyList());
                    Double avg = list.stream()
                            .map(HeartRate::getStressEma)
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(Double.NaN);
                    Float avgStress = Double.isNaN(avg) ? null : (float) avg.doubleValue();

                    return new DayStressDto(
                            dow.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                            avgStress
                    );
                })
                .collect(Collectors.toList());

        return new WeeklyStressResponse(days);
    }
}
