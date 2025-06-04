package com.example.Easeplan.api.Mypage.service;

import com.example.Easeplan.api.Calendar.repository.GoogleCalendarRepository;
import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.dto.HeartRateRequest;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.RefreshTokenRepository;
import com.example.Easeplan.global.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final GoogleCalendarRepository calendarRepo;
    private final SmartwatchRepository smartwatchRepo;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // 구글 캘린더 이벤트 저장
//    @Transactional
//    public void saveCalendarEvent(User user, CalendarEventRequest request) {
//        GoogleCalendarInfo event = GoogleCalendarInfo.builder()
//                .Id(request.Id())
//                .user(user)
//                .title(request.title())
//                .description(request.description())
//                .startDateTime(request.start())
//                .endDateTime(request.end())
//                .build();
//        calendarRepo.save(event);
//    }

    // 스마트워치 데이터 저장
// SmartwatchService.java (정상 코드)
    public void connectDevice(User user, HeartRateRequest request) {
        HeartRate data = HeartRate.builder() // ✅ 엔티티 변환
                .user(user)
                .build();
        smartwatchRepo.save(data);
    }

    // 회원 탈퇴
    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        refreshTokenRepository.deleteByEmail(email); // 리프레시 토큰 삭제
        userRepository.delete(user); // 회원 삭제 (연관 데이터 자동 삭제)
    }


    // 로그아웃 (리프레시 토큰만 삭제)
    @Transactional
    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }
}
