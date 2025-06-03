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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LongService {

    private final LongRepository longRepository;
    private final GoogleCalendarService googleCalendarService; // 캘린더 연동 서비스 주입
    private final UserRepository userRepository; // 유저 조회용
    private static final Logger log = LoggerFactory.getLogger(LongService.class);


    @Value("${culture.api.service-key}")
    private String serviceKey;



    private LocalDate safeParseDate(String dateStr) {
        try {
            if (dateStr.contains("T")) return LocalDate.parse(dateStr.substring(0, 10));
            if (dateStr.contains(" ")) return LocalDate.parse(dateStr.substring(0, 10));
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.error("날짜 파싱 실패: {}", dateStr);
            return LocalDate.now();
        }
    }
    // 5. 메인 서비스 메서드
    public List<RecommendationOption> getLongRecommendations(String email) {
        List<RecommendationOption> result = new ArrayList<>();
        List<FormattedTimeSlot> calendarEvents = getTodayCalendarEvents(email);
        List<FormattedTimeSlot> availableSlots = getAvailableSlots(calendarEvents);
        List<Event> todayEvents = getTodayEvents();
        List<RecommendationOption> eventOptions = pickTwoDifferentGenres(todayEvents, availableSlots);

        result.add(new RecommendationOption("calendar", "추천X(캘린더)", calendarEvents, null, null));

        for (RecommendationOption rec : eventOptions) {
            @SuppressWarnings("unchecked")
            List<FormattedTimeSlot> data = (List<FormattedTimeSlot>) rec.getData();
            List<FormattedTimeSlot> combined = new ArrayList<>(calendarEvents);
            combined.addAll(data);
            result.add(new RecommendationOption("event", rec.getLabel(), combined, rec.getStartTime(), rec.getEndTime()));
        }

        return result;
    }




    // === 아래는 예시 로직, 실제 구현 필요 ===

    // 구글 캘린더에서 오늘 일정 불러오기 (FormattedTimeSlot 리스트)
    // 1. 오늘의 캘린더 일정 가져오기
    private List<FormattedTimeSlot> getTodayCalendarEvents(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        ZoneId zone = ZoneId.of("Asia/Seoul");
        String timeMin = today.atStartOfDay(zone).format(formatter);
        String timeMax = today.plusDays(1).atStartOfDay(zone).format(formatter);

        try {
            return googleCalendarService.getFormattedEvents(user, "primary", timeMin, timeMax);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    // 오늘의 빈 시간(1시간 이상) 구하기 (FormattedTimeSlot 리스트)
    // 2. 오늘의 빈 시간(1시간 이상) 구하기 (08:00~22:00)
    // 1. 빈 시간 계산 시 08:00 이전 시간 제외하도록 조정 (이미 반영돼 있지만 재확인)
    private List<FormattedTimeSlot> getAvailableSlots(List<FormattedTimeSlot> calendarEvents) {
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.of("Asia/Seoul");

        LocalTime dayStart = LocalTime.of(8, 0);
        LocalTime dayEnd = LocalTime.of(22, 0);

        // 1. 08:00~22:00 안에 포함된 일정만 필터링
        List<LocalTime[]> occupied = calendarEvents.stream()
                .map(e -> {
                    ZonedDateTime start = ZonedDateTime.parse(e.getStartTime());
                    ZonedDateTime end = ZonedDateTime.parse(e.getEndTime());
                    return new LocalTime[] { start.toLocalTime(), end.toLocalTime() };
                })
                .filter(times -> !(times[1].isBefore(dayStart) || times[0].isAfter(dayEnd))) // 범위 밖 일정 제거
                .map(times -> {
                    // 시작 시간이 08:00보다 이르면 08:00으로 조정, 끝 시간이 22:00보다 늦으면 22:00으로 조정
                    LocalTime start = times[0].isBefore(dayStart) ? dayStart : times[0];
                    LocalTime end = times[1].isAfter(dayEnd) ? dayEnd : times[1];
                    return new LocalTime[] { start, end };
                })
                .sorted(Comparator.comparing(a -> a[0]))
                .collect(Collectors.toList());

        List<FormattedTimeSlot> slots = new ArrayList<>();
        LocalTime prevEnd = dayStart;

        for (LocalTime[] occ : occupied) {
            // 빈 구간을 1시간 단위로 채움
            while (!prevEnd.plusHours(1).isAfter(occ[0])) {
                LocalDateTime slotStart = LocalDateTime.of(today, prevEnd);
                LocalDateTime slotEnd = slotStart.plusHours(1);

                slots.add(new FormattedTimeSlot(
                        "추천 가능 시간",
                        "캘린더 빈 시간",
                        slotStart.atZone(zone).toString(),
                        slotEnd.atZone(zone).toString()
                ));
                prevEnd = prevEnd.plusHours(1);
            }
            prevEnd = occ[1].isAfter(prevEnd) ? occ[1] : prevEnd;
        }

        // 마지막 일정 이후 ~ 22시까지
        while (!prevEnd.plusHours(1).isAfter(dayEnd)) {
            LocalDateTime slotStart = LocalDateTime.of(today, prevEnd);
            LocalDateTime slotEnd = slotStart.plusHours(1);

            slots.add(new FormattedTimeSlot(
                    "추천 가능 시간",
                    "캘린더 빈 시간",
                    slotStart.atZone(zone).toString(),
                    slotEnd.atZone(zone).toString()
            ));
            prevEnd = prevEnd.plusHours(1);
        }

        return slots;
    }


    // 오늘 날짜 전체 행사 불러오기 (JSON 파싱)
    // 3. 오늘 날짜 전체 행사 불러오기 (JSON 파싱)
    private List<Event> getTodayEvents() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String apiUrl = String.format("http://openapi.seoul.go.kr:8088/%s/json/culturalEventInfo/1/1000/%%20/%%20/%s", serviceKey, today);
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            BufferedReader rd = new BufferedReader(new InputStreamReader(
                    conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300 ? conn.getInputStream() : conn.getErrorStream()));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) sb.append(line);
            rd.close();
            conn.disconnect();

            CulturalEventInfoRoot root = new ObjectMapper().readValue(sb.toString(), CulturalEventInfoRoot.class);
            return root.getCulturalEventInfo().getRow();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 장르별로 2개 추천 (빈 시간에 1시간씩 배정, FormattedTimeSlot으로 생성)
    // 4. 장르별로 2개 추천 (전체 빈 시간대 중 1시간만 긴 추천에 배정, 장소 description)

    // 2. 추천 시간 선택 시 시작 시간이 반드시 08:00 이후가 되도록 필터
// 장르별로 2개 추천 (빈 시간에 1시간씩 배정, FormattedTimeSlot으로 생성)
// 수정할 메서드만 아래에 다시 작성
// pickTwoDifferentGenres 메서드 수정 버전
// pickTwoDifferentGenres 전체 코드 수정 버전 (08:00~22:00 추천 시간 제한 포함)
// LongService.java 안의 pickTwoDifferentGenres 메서드 수정 버전
    private List<RecommendationOption> pickTwoDifferentGenres(List<Event> events, List<FormattedTimeSlot> slots) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        ZoneId zone = ZoneId.of("Asia/Seoul");

        // 오늘 날짜에 해당하는 행사만 필터링
        List<Event> todayEvents = events.stream()
                .filter(e -> !today.isBefore(safeParseDate(e.getStrtdate())) && !today.isAfter(safeParseDate(e.getEndDate())))
                .collect(Collectors.toList());

        // 08:00~22:00 내의 1시간 이상 빈 시간만 필터링
        List<FormattedTimeSlot> oneHourSlots = slots.stream()
                .map(slot -> {
                    try {
                        ZonedDateTime start = ZonedDateTime.parse(slot.getStartTime());
                        ZonedDateTime end = ZonedDateTime.parse(slot.getEndTime());

                        if (Duration.between(start, end).toMinutes() < 60) return null;
                        if (start.toLocalTime().isBefore(LocalTime.of(8, 0)) || end.toLocalTime().isAfter(LocalTime.of(22, 0)))
                            return null;

                        return slot;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 추천할 시간 슬롯이 없는 경우 빈 리스트 반환
        if (oneHourSlots.isEmpty()) return new ArrayList<>();

        // 하나의 추천 시간만 사용 (두 추천을 동일 시간에)
        FormattedTimeSlot sharedSlot = oneHourSlots.get(0);

        // 장르별로 묶기
        Map<String, List<Event>> genreMap = todayEvents.stream()
                .collect(Collectors.groupingBy(Event::getCodename));

        List<String> shuffledGenres = new ArrayList<>(genreMap.keySet());
        Collections.shuffle(shuffledGenres);

        List<RecommendationOption> options = new ArrayList<>();
        int count = 0;

        for (String genre : shuffledGenres) {
            if (count >= 2) break;

            List<Event> genreEvents = genreMap.get(genre);
            if (genreEvents == null || genreEvents.isEmpty()) continue;

            Event event = genreEvents.get(0); // 장르 내 첫 번째 이벤트 선택

            ZonedDateTime recoStart = ZonedDateTime.parse(sharedSlot.getStartTime());
            ZonedDateTime recoEnd = recoStart.plusHours(1);

            String formattedStart = recoStart.withZoneSameInstant(zone).format(formatter);
            String formattedEnd = recoEnd.withZoneSameInstant(zone).format(formatter);

            FormattedTimeSlot eventSlot = new FormattedTimeSlot(
                    event.getTitle(),
                    event.getPlace(),
                    formattedStart,
                    formattedEnd
            );

            options.add(new RecommendationOption(
                    "event",
                    genre,
                    Collections.singletonList(eventSlot),
                    formattedStart,
                    formattedEnd
            ));

            count++;
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
    public List<RecommendationOption> recommendForTomorrow(String email, Double latitude, Double longitude) {
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

        Optional<HeartRate> recentOpt = smartwatchRepository.findTopByUserEmailOrderByStartTimeDesc(email);
        Float stress = recentOpt.map(HeartRate::getAvg).orElse(null);

        List<RecommendationResult> recommends = getRecommendations(title, genre, stress, latitude, longitude);

        LocalDate today = LocalDate.now();
        String timeMin = today.atStartOfDay(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        String timeMax = today.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

        List<FormattedTimeSlot> calendarEvents;
        try {
            calendarEvents = googleCalendarService.getFormattedEvents(user, "primary", timeMin, timeMax);
        } catch (Exception e) {
            calendarEvents = new ArrayList<>();
        }

        List<FormattedTimeSlot> availableSlots;
        try {
            availableSlots = googleCalendarService.getFormattedFreeTimeSlots(user, today);
        } catch (Exception e) {
            availableSlots = new ArrayList<>();
        }

        // 필터: 08:00 ~ 22:00 사이의 1시간 이상 슬롯만 유지
        List<FormattedTimeSlot> filteredSlots = availableSlots.stream()
                .map(slot -> {
                    try {
                        ZonedDateTime start = ZonedDateTime.parse(slot.getStartTime());
                        ZonedDateTime end = ZonedDateTime.parse(slot.getEndTime());
                        if (start.toLocalTime().isBefore(LocalTime.of(8, 0)) || end.toLocalTime().isAfter(LocalTime.of(22, 0))) return null;
                        if (Duration.between(start, end).toMinutes() < 60) return null;
                        return slot;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (filteredSlots.isEmpty()) {
            throw new RuntimeException("추천을 배정할 수 있는 빈 시간대가 1개 이상 필요합니다.");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        ZoneId zone = ZoneId.of("Asia/Seoul");

        List<RecommendationOption> scenarios = new ArrayList<>();

        // 캘린더만
        scenarios.add(new RecommendationOption(
                "calendar",
                "추천X(캘린더)",
                new ArrayList<>(calendarEvents),
                null,
                null
        ));

        FormattedTimeSlot slot = filteredSlots.get(0);  // 하나의 빈 시간 슬롯 사용

        for (int i = 0; i < 2 && i < recommends.size(); i++) {
            ZonedDateTime slotStart = ZonedDateTime.parse(slot.getStartTime());
            ZonedDateTime slotEnd = ZonedDateTime.parse(slot.getEndTime());

            long slotMinutes = Duration.between(slotStart, slotEnd).toMinutes();
            long offset = slotMinutes > 60 ? new Random().nextInt((int)(slotMinutes - 60 + 1)) : 0;

            ZonedDateTime recoStart = slotStart.plusMinutes(offset);
            ZonedDateTime recoEnd = recoStart.plusHours(1);

            String formattedStart = recoStart.withZoneSameInstant(zone).format(formatter);
            String formattedEnd = recoEnd.withZoneSameInstant(zone).format(formatter);

            FormattedTimeSlot recommendedSlot = new FormattedTimeSlot(
                    recommends.get(i).getTitle(),
                    recommends.get(i).getDescription(),
                    formattedStart,
                    formattedEnd
            );

            List<FormattedTimeSlot> combined = new ArrayList<>(calendarEvents);
            combined.add(recommendedSlot);

            scenarios.add(new RecommendationOption(
                    "event",
                    recommends.get(i).getLabel(),
                    combined,
                    formattedStart,
                    formattedEnd
            ));
        }

        return scenarios;
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