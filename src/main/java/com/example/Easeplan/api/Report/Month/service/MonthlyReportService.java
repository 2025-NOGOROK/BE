package com.example.Easeplan.api.Report.Month.service;

import com.example.Easeplan.api.HaruRecord.domain.DailyEvaluation;
import com.example.Easeplan.api.HaruRecord.domain.Emotion;

import com.example.Easeplan.api.HaruRecord.repository.DailyEvaluationRepository;
import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.api.Report.Month.dto.EmotionPercentResponse;
import com.example.Easeplan.api.Report.Month.dto.DailyStressResponse;
import com.example.Easeplan.global.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyReportService {
    private final DailyEvaluationRepository dailyEvaluationRepository;
    private final SmartwatchRepository smartwatchRepository;

    // 감정별 비율
    public EmotionPercentResponse getEmotionPercent(User user, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // ✅ 타입을 DailyEvaluation으로!
        List<DailyEvaluation> records = dailyEvaluationRepository.findAllByUserAndDateBetween(user, start, end);

        Map<Emotion, Long> emotionCount = records.stream()
                .collect(Collectors.groupingBy(DailyEvaluation::getEmotion, Collectors.counting()));

        long total = records.size();
        Map<String, Double> percentMap = new LinkedHashMap<>();
        for (Emotion e : Emotion.values()) {
            double percent = total == 0 ? 0.0 : (emotionCount.getOrDefault(e, 0L) * 100.0) / total;
            percentMap.put(e.name(), percent);
        }
        return new EmotionPercentResponse(percentMap);
    }


    // 날짜별 하루 평균 스트레스 + 이모티콘
    public DailyStressResponse getDailyStress(User user, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 1. 월간 HeartRate 데이터 조회
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        List<HeartRate> heartRates = smartwatchRepository.findByUserAndStartTimeBetween(
                user,
                start.atStartOfDay().format(formatter),
                end.atTime(23, 59, 59).format(formatter)
        );

        // 2. 날짜별 그룹화
        Map<String, List<HeartRate>> dailyMap = heartRates.stream()
                .collect(Collectors.groupingBy(hr -> hr.getStartTime().substring(0, 10)));

        // 3. 날짜별 평균 및 이모티콘 변환
        List<DailyStressResponse.DailyStress> dailyStressList = new ArrayList<>();
        for (String date : dailyMap.keySet()) {
            List<HeartRate> list = dailyMap.get(date);
            double avg = list.stream()
                    .filter(hr -> hr.getAvg() != null)
                    .mapToDouble(HeartRate::getAvg)
                    .average()
                    .orElse(0.0);
            String emoji = getStressEmoji(avg);
            dailyStressList.add(new DailyStressResponse.DailyStress(date, avg, emoji));
        }
        // 날짜순 정렬
        dailyStressList.sort(Comparator.comparing(DailyStressResponse.DailyStress::getDate));
        return new DailyStressResponse(dailyStressList);
    }

    // 이모티콘 매핑 (예: 0~20: 😄, 21~40: 🙂, 41~60: 😐, 61~80: 😟, 81~100: 😫)
    private String getStressEmoji(double avg) {
        if (avg <= 20) return "20";
        else if (avg <= 40) return "40";
        else if (avg <= 60) return "60";
        else if (avg <= 80) return "80";
        else return "100";
    }
}
