package com.example.Easeplan.api.Emergency.controller;

import com.example.Easeplan.api.Emergency.domain.EmergencyStressEvent;
import com.example.Easeplan.api.Emergency.repository.EmergencyStressEventRepository;
import com.example.Easeplan.api.Emergency.service.ShortBreakWriter;
import com.example.Easeplan.api.ShortFlask.service.FlaskRecommendService;
import com.example.Easeplan.api.Survey.domain.UserSurvey;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.service.UserSurveyService;
import com.example.Easeplan.global.auth.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "긴급 스트레스 모드", description = "긴급 스트레스 모드 관련 API")

@RestController
@RequestMapping("/api/emergency-stress")
@RequiredArgsConstructor
public class EmergencyController {

    private final EmergencyStressEventRepository eventRepository;
    private final UserSurveyService surveyService;
    private final FlaskRecommendService flaskService;
    private final ShortBreakWriter shortBreakWriter;

    /**
     * 앱 시작 시 PENDING 이벤트 존재 여부 확인 → 있으면 전용 화면 띄우기
     */
    @Operation(
            summary = "앱 실행 시: 대기(PENDING) 중인 긴급 스트레스 이벤트 확인",
            description = """
프론트에서 **앱 실행 직후(또는 포그라운드 복귀 시) 1회** 호출해,
사용자가 푸시를 놓쳤더라도 긴급 스트레스 안내 화면을 띄울 수 있도록 합니다.

[언제 호출하나요?]
- 앱 콜드 스타트 직후 시 1회


[무엇을 응답하나요?]
- **pending = true**: 아직 활성화되지 않은(PENDING) 이벤트가 존재
  - 함께 내려오는 **eventId**로 전용 화면을 열고, 활성화 버튼에서
    `POST /api/emergency-stress/{eventId}/activate` 호출
- **pending = false**: 처리할 이벤트 없음(전용 화면 미표시)

[주의/동작 규칙]
- 한 사용자에 대해 PENDING은 1건만 존재한다고 가정합니다.
- 사용자가 활성화에 성공하면 해당 이벤트는 ACTIVATED로 바뀌고,
  이후 이 API는 **pending=false**를 반환합니다.
"""

    )
    @GetMapping("/pending")
    public ResponseEntity<?> hasPending(@AuthenticationPrincipal User me) {
        return eventRepository
                .findTopByUserAndStatusOrderByCreatedAtDesc(me, EmergencyStressEvent.Status.PENDING)
                .map(ev -> ResponseEntity.ok(Map.of(
                        "pending", true,
                        "eventId", ev.getId()
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "pending", false
                )));
    }


    /**
     * 활성화 버튼 클릭(딥링크/화면 버튼) → 오늘 포함 3일 배정
     */

    @Operation(
            summary = "긴급 스트레스 모드 활성화(멱등)",
            description = """
**특정 eventId**의 긴급 스트레스 이벤트를 **PENDING → ACTIVATED**로 전환하고, 
**오늘 포함 3일간 '짧은 쉼표' 일정을 자동 배정**합니다.

### 동작 요약
- 상태가 **PENDING** 인 경우에만 활성화가 수행되며, 배정 로직이 실행됩니다.
- **이미 ACTIVATED** 된 같은 이벤트(eventId)에 대해 **중복 호출**되면,
  일정은 **추가 생성하지 않고** 200 OK("이미 활성화 처리되었습니다.")만 반환합니다. (멱등)

### 클라이언트 권장 흐름
- FCM data-only에 포함된 `deeplink`(예: `nogorok://stress/chronic?eventId=...`)로 페이지 진입
- 쿼리의 `eventId`를 사용해 본 API 호출
- 만약 eventId를 못 받았다면 `/pending`으로 조회 후 얻은 eventId 사용

※ 활성화 시 설문 정보가 없으면 일정 배정은 생략하고 상태만 ACTIVATED로 바뀝니다.
"""

    )
    @PostMapping("/{eventId}/activate")
    @Transactional
    public ResponseEntity<?> activate(@AuthenticationPrincipal User me, @PathVariable Long eventId) {
        EmergencyStressEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid eventId"));

        if (!event.getUser().getId().equals(me.getId())) {
            return ResponseEntity.status(403).body("권한이 없습니다.");
        }

        // 멱등 처리: 이미 ACTIVATED면 OK로 응답
        if (event.getStatus() == EmergencyStressEvent.Status.ACTIVATED) {
            return ResponseEntity.ok("이미 활성화 처리되었습니다.");
        }
        if (event.getStatus() != EmergencyStressEvent.Status.PENDING) {
            return ResponseEntity.badRequest().body("처리할 수 없는 이벤트 상태입니다.");
        }

        // 설문 로드 (null 대비)
        UserSurvey survey = surveyService.getSurveyByUser(me);
        if (survey == null) {
            // 설문이 없으면 일정만 생략하고 상태만 ACTIVATED로 바꿔도 됨
            event.markActivated();
            eventRepository.save(event);
            return ResponseEntity.ok("설문 정보가 없어 일정을 배정하지 않았지만, 활성화는 완료되었습니다.");
        }

        UserSurveyRequest req = UserSurveyRequest.fromEntity(survey);
        List<String> dailyTitles = flaskService.getRecommendations(req).stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .limit(3)
                // .collect(Collectors.toList()); // Java 11
                .toList();                         // Java 16+

        if (!dailyTitles.isEmpty()) {
            shortBreakWriter.createShortBreakNDays(me, LocalDate.now(), dailyTitles);
        }

        event.markActivated();
        eventRepository.save(event);

        return ResponseEntity.ok("긴급 스트레스 모드를 활성화했고, 오늘 포함 3일간 짧은 쉼표를 배정했습니다.");
    }

}

