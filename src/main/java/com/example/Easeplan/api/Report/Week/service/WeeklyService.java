package com.example.Easeplan.api.Report.Week.service;

import com.example.Easeplan.api.HaruRecord.domain.DailyEvaluation;
import com.example.Easeplan.api.HaruRecord.repository.DailyEvaluationRepository;
import com.example.Easeplan.api.Report.Week.dto.*;
import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // DB에서 해당 주간의 모든 HeartRate 데이터 조회
        List<HeartRate> heartRates = smartwatchRepository.findByUserAndStartTimeBetween(
                user,
                startOfWeek.toString(), // "yyyy-MM-dd"
                endOfWeek.toString()
        );

        // 요일별 그룹핑
        Map<DayOfWeek, List<HeartRate>> byDay = heartRates.stream()
                .collect(Collectors.groupingBy(hr -> {
                    // startTime이 "yyyy-MM-dd..." 형식이라고 가정
                    LocalDate date = LocalDate.parse(hr.getStartTime().substring(0, 10));
                    return date.getDayOfWeek();
                }));

        List<DayStressDto> days = Arrays.stream(DayOfWeek.values())
                .map(dow -> {
                    List<HeartRate> list = byDay.getOrDefault(dow, Collections.emptyList());
                    float sum = 0f;
                    int totalCount = 0;
                    for (HeartRate hr : list) {
                        if (hr.getAvg() != null && hr.getCount() != null) {
                            sum += hr.getAvg() * hr.getCount();
                            totalCount += hr.getCount();
                        }
                    }
                    Float avgStress = totalCount > 0 ? sum / totalCount : null;
                    return new DayStressDto(
                            dow.getDisplayName(TextStyle.SHORT, Locale.KOREAN), avgStress
                    );
                })
                .collect(Collectors.toList());

        return new WeeklyStressResponse(days);
    }
}
