//package com.example.Easeplan.api.Scenario.service;
//
//import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
//import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
//import com.example.Easeplan.api.Scenario.record.ScheduleScenario;
//import com.example.Easeplan.api.ShortFlask.service.FlaskRecommendService;
//import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
//import com.example.Easeplan.global.auth.domain.User;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ScenarioService {
//    private final GoogleCalendarService calendarService;
//    private final FlaskRecommendService flaskService;
//
//    // 1. 구글 캘린더 기반 시나리오
//    public ScheduleScenario generateGoogleScenario(User user) {
//        try {
//            List<FormattedTimeSlot> slots = calendarService.getFormattedFreeTimeSlots(
//                    user.getGoogleAccessToken(),
//                    user.getGoogleRefreshToken(),
//                    LocalDate.now()
//            );
//            return new ScheduleScenario("GOOGLE", slots, List.of("캘린더 기반 일정"));
//        } catch (Exception e) {
//            log.error("구글 캘린더 조회 실패: {}", e.getMessage());
//            return new ScheduleScenario("GOOGLE", List.of(), List.of("캘린더 데이터를 불러올 수 없습니다"));
//        }
//    }
//
//    // 2. 설문조사 기반 시나리오
//    public ScheduleScenario generateSurveyScenario(User user, UserSurveyRequest request) {
//        try {
//            List<String> recommendations = flaskService.getRecommendations(request);
//            List<FormattedTimeSlot> slots = calendarService.getFormattedFreeTimeSlots(
//                    user.getGoogleAccessToken(),
//                    user.getGoogleRefreshToken(),
//                    LocalDate.now()
//            );
//            return new ScheduleScenario("SURVEY", slots, recommendations);
//        } catch (Exception e) {
//            log.error("설문 기반 추천 실패: {}", e.getMessage());
//            return new ScheduleScenario("SURVEY", List.of(), List.of("추천 데이터를 생성할 수 없습니다"));
//        }
//    }
//
//    // 3. 스트레스 분석 기반 시나리오
//    public ScheduleScenario generateStressScenario(User user) {
//        try {
//            List<String> recommendations = stressService.analyzeStress(user);
//            List<FormattedTimeSlot> slots = calendarService.getFormattedFreeTimeSlots(
//                    user.getGoogleAccessToken(),
//                    user.getGoogleRefreshToken(),
//                    LocalDate.now()
//            );
//            return new ScheduleScenario("STRESS", slots, recommendations);
//        } catch (Exception e) {
//            log.error("스트레스 분석 실패: {}", e.getMessage());
//            return new ScheduleScenario("STRESS", List.of(), List.of("스트레스 관리 활동을 추천할 수 없습니다"));
//        }
//    }
//}
