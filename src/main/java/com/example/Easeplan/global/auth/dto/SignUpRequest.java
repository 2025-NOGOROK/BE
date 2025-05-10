package com.example.Easeplan.global.auth.dto;

public record SignUpRequest(
        String name,           // 사용자 이름 (필수)
        String birth,          // 생년월일 (필수, "yyyy-MM-dd" 등)
        String gender,         // 성별 (선택, "M" or "F" 등, null 허용)

        String password,
        String email,

        String confirmPassword, // 비밀번호 확인 (필수)

        boolean pushNotificationAgreed, //알림
        String deviceToken, // 추가 (푸시 토큰)


        // 약관 동의 필드 추가
        boolean termsOfServiceAgreed,      // 이용약관 동의
        boolean privacyPolicyAgreed,       // 개인정보 수집 및 이용 동의
        boolean healthInfoPolicyAgreed,    // 건강정보 수집 및 이용 동의
        boolean locationPolicyAgreed       // 위치기반 서비스 이용 동의

) {

}
