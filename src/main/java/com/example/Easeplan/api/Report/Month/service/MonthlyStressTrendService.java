package com.example.Easeplan.api.Report.Month.service;
import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.Report.Month.dto.MonthlyStressPoint;
import com.example.Easeplan.api.Report.Month.dto.MonthlyStressTrendResponse;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MonthlyStressTrendService {

    private final SmartwatchRepository smartwatchRepository;
    private final UserRepository userRepository;

    public MonthlyStressTrendResponse getRecentMonthlyStress(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ZoneId KST = ZoneId.of("Asia/Seoul");
        YearMonth cur = YearMonth.now(KST);

        List<MonthlyStressPoint> points = new ArrayList<>();

        // 1) 이번 달: 데이터 유무와 관계없이 반드시 추가
        points.add(buildMonthlyPoint(email, cur)); // 없으면 0으로 세팅

        // 2) 직전/전전 달: 데이터가 있는 경우만 붙임 (최대 2개)
        for (int i = 1; i <= 2; i++) {
            YearMonth target = cur.minusMonths(i);
            String prefix = target.toString(); // "yyyy-MM"
            if (!smartwatchRepository.existsByUserEmailAndStartTimeStartingWith(email, prefix)) {
                continue; // 데이터 없으면 건너뜀
            }
            points.add(buildMonthlyPoint(email, target));
        }

        // 오래된 → 최신 순으로 정렬
        points.sort((a, b) -> a.getYear() != b.getYear()
                ? Integer.compare(a.getYear(), b.getYear())
                : Integer.compare(a.getMonth(), b.getMonth()));

        return MonthlyStressTrendResponse.builder().points(points).build();
    }

    private MonthlyStressPoint buildMonthlyPoint(String email, YearMonth ym) {
        String prefix = ym.toString(); // "yyyy-MM"
        var list = smartwatchRepository.findByUserEmailAndStartTimeStartingWith(email, prefix);

        double avg = list.stream()
                .map(HeartRate::getAvg)
                .filter(Objects::nonNull)
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(Double.NaN);

        int value = Double.isNaN(avg) ? 0 : (int) Math.round(avg); // 데이터 없으면 0
        return MonthlyStressPoint.builder()
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .value(value)
                .build();
    }
}
