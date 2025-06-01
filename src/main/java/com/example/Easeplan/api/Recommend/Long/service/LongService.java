package com.example.Easeplan.api.Recommend.Long.service;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.api.Recommend.Long.RecommendationResult.RecommendationResult;
import com.example.Easeplan.api.Recommend.Long.dto.CulturalEventInfoRoot;
import com.example.Easeplan.api.Recommend.Long.dto.Event;
import com.example.Easeplan.api.Recommend.Long.dto.RecommendationOption;
import com.example.Easeplan.api.Recommend.Long.dto.UserChoice;
import com.example.Easeplan.api.Recommend.Long.repository.LongRepository;
import com.example.Easeplan.api.Recommend.Long.repository.UserChoiceRepository;
import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LongService {

    private final LongRepository longRepository;
    private final GoogleCalendarService googleCalendarService; // 캘린더 연동 서비스 주입
    private final UserRepository userRepository; // 유저 조회용


    @Value("${culture.api.service-key}")
    private String serviceKey;

    public List<RecommendationOption> getLongRecommendations(String email) {
        List<RecommendationOption> result = new ArrayList<>();

        // 1. 구글 캘린더에서 오늘 일정 가져오기 (FormattedTimeSlot 리스트로)
        List<FormattedTimeSlot> calendarEvents = getTodayCalendarEvents(email);

        // 2. 오늘의 빈 시간(1시간 이상) 구하기
        List<FormattedTimeSlot> availableSlots = getAvailableSlots(calendarEvents);

        // 3. 오늘 날짜 전체 행사 불러오기 (JSON 파싱)
        List<Event> todayEvents = getTodayEvents();

        // 4. 장르별로 2개 추천 (빈 시간에 1시간씩 배정, FormattedTimeSlot으로 생성)
        List<RecommendationOption> eventOptions = pickTwoDifferentGenres(todayEvents, availableSlots);

        // 5. 캘린더 일정만 보여주는 시나리오
        result.add(new RecommendationOption(
                "calendar",
                "추천X(캘린더)",
                calendarEvents,
                null,
                null
        ));

        // 6. 장르별 추천 2개: 각각 "캘린더 일정 전체 + 추천 일정 1개"로 반환
        for (RecommendationOption rec : eventOptions) {
            List<FormattedTimeSlot> combined = new ArrayList<>(calendarEvents);
            combined.add((FormattedTimeSlot) rec.getData());

            result.add(new RecommendationOption(
                    "event",
                    rec.getLabel(),
                    combined,
                    rec.getStartTime(),
                    rec.getEndTime()
            ));
        }

        return result;
    }




    // === 아래는 예시 로직, 실제 구현 필요 ===

    // 구글 캘린더에서 오늘 일정 불러오기 (FormattedTimeSlot 리스트)
    private List<FormattedTimeSlot> getTodayCalendarEvents(String email) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. 오늘 날짜 범위 계산 (RFC3339 포맷)
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        ZoneId zone = ZoneId.of("Asia/Seoul");
        String timeMin = today.atStartOfDay(zone).format(formatter); // 예: "2025-05-27T00:00:00+09:00"
        String timeMax = today.plusDays(1).atStartOfDay(zone).format(formatter); // 예: "2025-05-28T00:00:00+09:00"
        try {
            return googleCalendarService.getFormattedEvents(user, "primary", timeMin, timeMax);
        } catch (Exception e) {
            // 예외 처리: 로그를 남기고, 빈 리스트 반환 또는 적절한 예외 던지기
            e.printStackTrace();
            return new ArrayList<>(); // 또는 throw new RuntimeException("캘린더 조회 실패", e);
        }
    }


    // 오늘의 빈 시간(1시간 이상) 구하기 (FormattedTimeSlot 리스트)
    private List<FormattedTimeSlot> getAvailableSlots(List<FormattedTimeSlot> calendarEvents) {
        LocalDate today = LocalDate.now();
        LocalTime dayStart = LocalTime.of(8, 0);
        LocalTime dayEnd = LocalTime.of(22, 0);

        List<LocalTime[]> occupied = calendarEvents.stream()
                .map(e -> new LocalTime[]{
                        ZonedDateTime.parse(e.getStartTime()).toLocalTime(),
                        ZonedDateTime.parse(e.getEndTime()).toLocalTime()
                })
                .sorted(Comparator.comparing(a -> a[0]))
                .collect(Collectors.toList());


        List<FormattedTimeSlot> slots = new ArrayList<>();
        LocalTime prevEnd = dayStart;
        for (LocalTime[] occ : occupied) {
            if (prevEnd.isBefore(occ[0]) && Duration.between(prevEnd, occ[0]).toMinutes() >= 60) {
                slots.add(new FormattedTimeSlot(
                        "추천 가능 시간", "캘린더 빈 시간",
                        LocalDateTime.of(today, prevEnd).toString(),
                        LocalDateTime.of(today, prevEnd.plusHours(1)).toString()
                ));
            }
            prevEnd = occ[1].isAfter(prevEnd) ? occ[1] : prevEnd;
        }
        if (prevEnd.isBefore(dayEnd) && Duration.between(prevEnd, dayEnd).toMinutes() >= 60) {
            slots.add(new FormattedTimeSlot(
                    "추천 가능 시간", "캘린더 빈 시간",
                    LocalDateTime.of(today, prevEnd).toString(),
                    LocalDateTime.of(today, prevEnd.plusHours(1)).toString()
            ));
        }
        return slots;
    }

    // 오늘 날짜 전체 행사 불러오기 (JSON 파싱)
    private List<Event> getTodayEvents() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String apiUrl = String.format(
                    "http://openapi.seoul.go.kr:8088/%s/json/culturalEventInfo/1/1000/%%20/%%20/%s",
                    serviceKey, today

            );
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            BufferedReader rd;
            if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            conn.disconnect();

            ObjectMapper mapper = new ObjectMapper();
            CulturalEventInfoRoot root = mapper.readValue(sb.toString(), CulturalEventInfoRoot.class);
            List<Event> events = root.getCulturalEventInfo().getRow();

            // === 여기에 sout 추가 ===
            System.out.println("오늘 행사 개수: " + (events == null ? 0 : events.size()));
            if (events != null && !events.isEmpty()) {
                System.out.println("첫번째 행사: " + events.get(0).getCodename() + " / " + events.get(0).getTitle());
            }

            return events;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 장르별로 2개 추천 (빈 시간에 1시간씩 배정, FormattedTimeSlot으로 생성)
    private List<RecommendationOption> pickTwoDifferentGenres(List<Event> events, List<FormattedTimeSlot> slots) {
        Map<String, List<Event>> genreMap = events.stream()
                .collect(Collectors.groupingBy(Event::getCodename));
        List<String> genreKeys = new ArrayList<>(genreMap.keySet());
        Collections.shuffle(genreKeys);

        List<RecommendationOption> options = new ArrayList<>();
        int slotIdx = 0;
        int genreCount = 0;
        // 슬롯이 1개여도, 추천 2개(서로 다른 장르) 생성
        if (!slots.isEmpty()) {
            FormattedTimeSlot slot = slots.get(0); // 항상 첫 번째 슬롯 사용
            for (String genre : genreKeys) {
                List<Event> genreEvents = genreMap.get(genre);
                if (!genreEvents.isEmpty()) {
                    Event event = genreEvents.get(0);
                    FormattedTimeSlot eventSlot = new FormattedTimeSlot(
                            event.getTitle(),
                            event.getPlace(),
                            slot.getStartTime(),
                            slot.getEndTime()
                    );
                    options.add(new RecommendationOption(
                            "event",
                            genre,
                            eventSlot,
                            slot.getStartTime(),
                            slot.getEndTime()
                    ));
                    genreCount++;
                }
                if (genreCount == 2) break; // 2개만 추천
            }
        }
        return options;
    }
    @Autowired
    private ObjectMapper objectMapper;

    public void saveUserChoice(String email, RecommendationOption choice) {
        longRepository.saveUserChoice(email, choice);

        if ("event".equals(choice.getType())) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<FormattedTimeSlot> dataList;
            try {
                // data를 JSON 문자열로 직렬화한 뒤 다시 원하는 타입으로 역직렬화
                String json = objectMapper.writeValueAsString(choice.getData());
                dataList = objectMapper.readValue(json, new TypeReference<List<FormattedTimeSlot>>() {});
            } catch (Exception e) {
                throw new RuntimeException("추천 일정 변환 실패", e);
            }

            FormattedTimeSlot recommendedSlot = dataList.get(dataList.size() - 1);

            // 시간 포맷 변환 (RFC3339)
            String startTime = toRFC3339(recommendedSlot.getStartTime());
            String endTime = toRFC3339(recommendedSlot.getEndTime());

            System.out.println("구글 캘린더 저장 시도: " + recommendedSlot.getTitle() + " " + startTime + " ~ " + endTime);

            try {
                googleCalendarService.addEvent(
                        user,
                        "primary",
                        recommendedSlot.getTitle(),
                        recommendedSlot.getDescription(),
                        startTime,
                        endTime,
                        false,  // serverAlarm
                        0,      // minutesBeforeAlarm
                        false,  // fixed
                        false   // userLabel
                );
            } catch (Exception e) {
                throw new RuntimeException("구글 캘린더 저장 실패: " + e.getMessage(), e);
            }
        }
    }

    // 시간 변환 유틸 함수
    private String toRFC3339(String input) {
        if (input == null) return null;
        // 이미 RFC3339 포맷이면 그대로 반환
        if (input.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.+")) {
            return input;
        }
        try {
            // "yyyy-MM-dd'T'HH:mm" -> "yyyy-MM-dd'T'HH:mm:ssXXX"
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime ldt = LocalDateTime.parse(input, inputFormatter);
            ZonedDateTime zdt = ldt.atZone(ZoneId.of("Asia/Seoul"));
            return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception e) {
            // 혹시 다른 포맷이면 그대로 반환(최소한 에러는 막음)
            return input;
        }
    }





    @Autowired
    private UserChoiceRepository userChoiceRepository;
    @Autowired
    private SmartwatchRepository smartwatchRepository;

    //**“내일(오늘) 추천 공연을 사용자에게 제공”**하는 메인 서비스 메서드
    public List<RecommendationResult> recommendForTomorrow(String email,Double latitude, Double longitude) {
        // 1. 전날 type="event" 일정 추출
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String yesterdayStart = yesterday.atStartOfDay().toString();
        String yesterdayEnd = yesterday.atTime(23, 59, 59).toString();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserChoice> longChoices = userChoiceRepository
                .findByUserAndTypeAndStartTimeBetween(user, "event", yesterdayStart, yesterdayEnd);

        if (longChoices.isEmpty()) throw new RuntimeException("전날 긴 추천 없음");

        UserChoice lastLong = longChoices.get(longChoices.size() - 1);
        String title = lastLong.getEventTitle();
        String genre = lastLong.getLabel();

        // 2. 최근 스트레스 데이터 추출
        Optional<HeartRate> recentOpt = smartwatchRepository.findTopByUserEmailOrderByStartTimeDesc(email);
        Float stress = recentOpt.map(HeartRate::getAvg).orElse(null);

        // 3. Python 추천 서버 호출
        return getRecommendations(title, genre, stress, latitude, longitude);
    }

    //“어제 사용자가 실제로 선택한 긴 추천(공연)”을 DB에서 찾아오는 서비스 메서드
    public UserChoice getYesterdayLongChoice(String email) {
        // 어제 날짜 계산
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String yesterdayStart = yesterday.atStartOfDay().toString(); // "2025-05-27T00:00:00"
        String yesterdayEnd = yesterday.atTime(23, 59, 59).toString(); // "2025-05-27T23:59:59"

        // User 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 어제 type="event" 일정 조회
        List<UserChoice> longChoices = userChoiceRepository
                .findByUserAndTypeAndStartTimeBetween(user, "event", yesterdayStart, yesterdayEnd);

        if (longChoices.isEmpty()) throw new RuntimeException("어제 긴 추천 없음");

        return longChoices.get(longChoices.size() - 1); // 가장 마지막 저장된 긴 추천
    }




    //“공연명(title), 장르(genre), 스트레스(stress)를 Python 추천 서버에 전달하고,
    //추천 결과를 받아오는 서비스 메서드”
    @Value("${flask.long-reco}")
    private String pythonUrl;
    private List<RecommendationResult> getRecommendations(String title, String label, Float stress, Double latitude, Double longitude) {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        request.put("label", label);
        request.put("stress", stress);
        request.put("latitude", latitude);
        request.put("longitude", longitude);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<RecommendationResult[]> response =
                restTemplate.postForEntity(pythonUrl, entity, RecommendationResult[].class);

        return Arrays.asList(response.getBody());
    }
}