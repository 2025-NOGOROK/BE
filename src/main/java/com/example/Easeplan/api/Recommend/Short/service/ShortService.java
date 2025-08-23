package com.example.Easeplan.api.Recommend.Short.service;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.dto.TimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.api.ShortFlask.service.FlaskRecommendService;
import com.example.Easeplan.api.Survey.domain.UserSurvey;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.service.UserSurveyService;
import com.example.Easeplan.global.auth.domain.User;
import com.google.api.client.util.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ShortService {

    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
    private static final Pattern DURATION_PATTERN = Pattern.compile("\\((\\d+)분\\)");

    private final UserSurveyService surveyService;
    private final GoogleCalendarService calendarService;
    private final FlaskRecommendService flaskService;
    private final PairRotationState state;

    @Autowired
    public ShortService(UserSurveyService surveyService,
                        GoogleCalendarService calendarService,
                        FlaskRecommendService flaskService,
                        PairRotationState state) {
        this.surveyService = surveyService;
        this.calendarService = calendarService;
        this.flaskService = flaskService;
        this.state = state;
    }

    /** 한 번 호출 시 2개 일정 생성 + 다음 호출 시 다른 조합(쌍)으로 순환 (직전 항목 반복 방지) */
    public List<FormattedTimeSlot> generateTwoShortBreaks(User user, LocalDate date) throws Exception {
        // 1) 설문 조회
        UserSurvey survey = surveyService.getSurveyByUser(user);
        UserSurveyRequest request = UserSurveyRequest.fromEntity(survey);

        // 2) 하루 범위
        ZonedDateTime startOfDay = date.atStartOfDay(ZONE_KST);
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        // 3) 기존 캘린더
        List<FormattedTimeSlot> existing = calendarService.getFormattedEvents(
                user, "primary",
                startOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                endOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
        List<FormattedTimeSlot> all = new ArrayList<>(existing);

        // 4) 빈 슬롯 (06:00 이후)
        List<TimeSlot> freeSlots = calendarService.getFreeTimeSlots(user, date);
        List<TimeSlot> filteredSlots = freeSlots.stream()
                .filter(slot -> {
                    Instant si = Instant.ofEpochMilli(slot.getStart().getValue());
                    return si.atZone(ZONE_KST).toLocalTime().isAfter(LocalTime.of(5, 59));
                }).collect(Collectors.toList());

        // 최소 2개 보장
        while (filteredSlots.size() < 2) {
            ZonedDateTime fallbackStart = date.atTime(10 + filteredSlots.size(), 0).atZone(ZONE_KST);
            ZonedDateTime fallbackEnd = fallbackStart.plusMinutes(60);
            filteredSlots.add(new TimeSlot(
                    new DateTime(fallbackStart.toInstant().toEpochMilli()),
                    new DateTime(fallbackEnd.toInstant().toEpochMilli())
            ));
        }

        // 5) Flask 추천
        List<String> recs = flaskService.getRecommendations(request);
        if (recs == null) recs = Collections.emptyList();

        List<String> uniqueRecs = recs.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        final int K = Math.min(10, uniqueRecs.size()); // CSV에 10개 넣었다면 10C2=45쌍
        if (K < 2) return all;

        List<String> basis = uniqueRecs.subList(0, K);
        List<List<String>> pairs = buildPairs(basis); // nC2

        // 6) 상태 키
        String pairKey = buildPairKey(user, date, request);
        int cursor = state.getCursor(pairKey);
        Set<String> used = state.getUsedPairs(pairKey);
        Set<String> lastItems = state.getLastItems(pairKey); // 직전 호출의 두 제목

        // 7) 다음 쌍 선택: (미사용 && lastItems와 교집합 없음) 우선
        List<String> chosenPair = pickNextPairAvoidingLastItems(pairs, cursor, used, lastItems);

        // 없으면 미사용 아무거나
        if (chosenPair == null) {
            chosenPair = pickNextUnusedPair(pairs, cursor, used);
        }
        // 그래도 없으면 리셋 후 첫 번째
        if (chosenPair == null && !pairs.isEmpty()) {
            used.clear();
            chosenPair = pairs.get(0);
            markUsed(used, chosenPair);
            cursor = 1 % pairs.size();
        } else if (chosenPair != null) {
            cursor = (cursor + 1) % Math.max(1, pairs.size());
        }

        state.setCursor(pairKey, cursor);
        state.setUsedPairs(pairKey, used);

        // 8) 두 개 이벤트 생성
        List<FormattedTimeSlot> recommendEvents = new ArrayList<>();
        if (chosenPair != null) {
            for (int i = 0; i < 2; i++) {
                String title = chosenPair.get(i);
                int durationMinutes = parseDurationMinutes(title, 60);

                TimeSlot slot = filteredSlots.get(i);
                ZonedDateTime startZ = Instant.ofEpochMilli(slot.getStart().getValue()).atZone(ZONE_KST);
                ZonedDateTime endZ = startZ.plusMinutes(durationMinutes);

                DateTime start = new DateTime(startZ.toInstant().toEpochMilli());
                DateTime end = new DateTime(endZ.toInstant().toEpochMilli());

                FormattedTimeSlot event = new FormattedTimeSlot(
                        title, "설문 기반 추천",
                        start.toStringRfc3339(),
                        end.toStringRfc3339(),
                        "short-recommend"
                );

                calendarService.addEvent(
                        user, "primary",
                        event.getTitle(),
                        event.getDescription(),
                        start.toStringRfc3339(),
                        end.toStringRfc3339(),
                        false, 0, false, false, "short-recommend"
                );
                recommendEvents.add(event);
            }

            // 히스토리 (옵션)
            String historyKey = user.getEmail() + "|" + date;
            Set<String> history = state.getHistory(historyKey);
            chosenPair.forEach(history::add);
            state.setHistory(historyKey, history);

            // ✅ 직전 두 제목을 저장 → 다음 호출에서 반복 회피
            state.setLastItems(pairKey, new HashSet<>(chosenPair));
        }

        all.addAll(recommendEvents);
        return all;
    }

    // helpers
    private List<List<String>> buildPairs(List<String> items) {
        List<List<String>> pairs = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            for (int j = i + 1; j < items.size(); j++) {
                pairs.add(Arrays.asList(items.get(i), items.get(j)));
            }
        }
        return pairs;
    }

    private String buildPairKey(User user, LocalDate date, UserSurveyRequest req) {
        return String.join("|",
                user.getEmail(),
                date.toString(),
                String.valueOf(req.getScheduleType()),
                String.valueOf(req.getSuddenChangePreferred()),
                String.valueOf(req.getChronotype()),
                String.valueOf(req.getPreferAlone()),
                String.valueOf(req.getStressReaction())
        );
    }

    /** lastItems와 교집합이 없는 '미사용' 쌍 우선 선택 */
    private List<String> pickNextPairAvoidingLastItems(
            List<List<String>> pairs, int startCursor, Set<String> used, Set<String> lastItems) {
        if (pairs.isEmpty()) return null;
        int cursor = startCursor;
        int attempts = 0;

        while (attempts < pairs.size()) {
            List<String> candidate = pairs.get(cursor % pairs.size());
            String pairId = normalizedPairId(candidate);

            boolean unused = !used.contains(pairId);
            boolean disjointFromLast = (lastItems == null || lastItems.isEmpty()
                    || Collections.disjoint(lastItems, candidate));

            if (unused && disjointFromLast) {
                used.add(pairId);
                return candidate;
            }
            cursor = (cursor + 1) % pairs.size();
            attempts++;
        }
        return null;
    }

    private List<String> pickNextUnusedPair(List<List<String>> pairs, int startCursor, Set<String> used) {
        if (pairs.isEmpty()) return null;
        int cursor = startCursor;
        int attempts = 0;
        while (attempts < pairs.size()) {
            List<String> candidate = pairs.get(cursor % pairs.size());
            String pairId = normalizedPairId(candidate);
            if (!used.contains(pairId)) {
                used.add(pairId);
                return candidate;
            }
            cursor = (cursor + 1) % pairs.size();
            attempts++;
        }
        return null;
    }

    private void markUsed(Set<String> used, List<String> pair) {
        used.add(normalizedPairId(pair));
    }

    private String normalizedPairId(List<String> pair) {
        List<String> sorted = new ArrayList<>(pair);
        Collections.sort(sorted);
        return sorted.get(0) + "||" + sorted.get(1);
    }

    private int parseDurationMinutes(String title, int defaultMinutes) {
        Matcher m = DURATION_PATTERN.matcher(title);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return defaultMinutes;
    }
}
