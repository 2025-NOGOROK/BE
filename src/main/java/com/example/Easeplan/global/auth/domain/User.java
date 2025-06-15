package com.example.Easeplan.global.auth.domain;

import com.example.Easeplan.api.Calendar.domain.GoogleCalendarInfo;
import com.example.Easeplan.api.HaruRecord.domain.DailyEvaluation;
import com.example.Easeplan.api.Recommend.Long.dto.UserChoice;
import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.Survey.domain.UserSurvey;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime; // LocalDateTime 임포트
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// 아래 임포트는 실제 코드에 없었지만, 있다면 주석처리 또는 제거 여부 판단
// import com.example.Easeplan.api.Calendar.domain.GoogleCalendarInfo; // User 엔티티에서 직접 List<GoogleCalendarInfo>를 관리한다면 필요
// import com.example.Easeplan.api.SmartWatch.domain.HeartRate; // User 엔티티에서 직접 List<HeartRate>를 관리한다면 필요
// import com.example.Easeplan.api.Survey.domain.UserSurvey; // User 엔티티에서 직접 UserSurvey를 관리한다면 필요
@Setter
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(name = "`user`") // SQL 예약어 'user' 충돌 방지를 위해 백틱 사용
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String birth;

    @Column
    private String gender;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email; // UserDetails의 username으로 사용

    @Column(nullable = false)
    private boolean pushNotificationAgreed;

    @Column
    private String deviceToken; // 단일 FCM 기기 토큰 (여러 기기라면 fcmTokens 리스트 사용)

    // 약관 동의 필드
    @Column(nullable = false)
    private boolean termsOfServiceAgreed;

    @Column(nullable = false)
    private boolean privacyPolicyAgreed;

    @Column(nullable = false)
    private boolean healthInfoPolicyAgreed;

    @Column(nullable = false)
    private boolean locationPolicyAgreed;

    // UserSurvey 관계 (mappedBy 속성 주의)
    // UserSurvey 엔티티에 'user' 필드가 있어야 합니다.
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSurvey userSurvey; // UserSurvey 엔티티 클래스가 존재해야 함


    @Column(name = "google_auth_code")
    private String googleAuthCode;  // 구글 OAuth 코드 추가



    @Column(name = "google_access_token")
    private String googleAccessToken;

    @Column(name = "google_refresh_token")
    private String googleRefreshToken;

    // **[핵심 변경]** 구글 액세스 토큰 만료 시각 저장 필드 추가 (UTC 기준)
    @Column(name = "google_access_token_expires_at")
    private LocalDateTime googleAccessTokenExpiresAt;

    /**
     * 구글 OAuth 토큰 정보를 업데이트합니다.
     * @param newAccessToken 새로 발급받은 액세스 토큰
     * @param newRefreshToken 새로 발급받은 리프레시 토큰 (없으면 null 또는 빈 문자열)
     */
    public void updateGoogleTokens(String newAccessToken, String newRefreshToken, LocalDateTime expiresAt) {
        this.googleAccessToken = newAccessToken;

        // refresh token은 최초 발급 시 또는 아주 드물게 갱신될 때만 넘어옵니다.
        if (newRefreshToken != null && !newRefreshToken.isEmpty()) {
            this.googleRefreshToken = newRefreshToken;
        }

        // googleAccessTokenExpiresAt을 설정합니다.
        this.googleAccessTokenExpiresAt = expiresAt;
    }


    /**
     * 구글 액세스 토큰의 만료 시각을 설정합니다. (UTC 기준)
     * @param expiresAt 만료 시각 (LocalDateTime)
     */
    public void setGoogleAccessTokenExpiresAt(LocalDateTime expiresAt) {
        this.googleAccessTokenExpiresAt = expiresAt;
    }

    // 비밀번호 설정 메서드
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email; // UserDetails의 username은 이메일로 매핑
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }


    // daily_evaluation
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyEvaluation> dailyEvaluations = new ArrayList<>();

    // 2. UserChoice
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserChoice> userChoices = new ArrayList<>();
    // GoogleCalendarInfo 와의 1:N 관계
    // GoogleCalendarInfo 엔티티에 'user' 필드가 있어야 합니다.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoogleCalendarInfo> calendarEvents = new ArrayList<>();

    // HeartRate 와의 1:N 관계
    // HeartRate 엔티티에 'user' 필드가 있어야 합니다.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HeartRate> smartwatchData = new ArrayList<>();

    // FCM 토큰 관리를 위한 컬렉션 (여러 기기 지원)
    @ElementCollection(fetch = FetchType.EAGER) // 즉시 로딩 (DB 쿼리 한 번으로 가져옴)
    @CollectionTable(name = "user_fcm_tokens", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "token")
    private List<String> fcmTokens = new ArrayList<>();

    public void addFcmToken(String token) {
        if (!fcmTokens.contains(token)) {
            fcmTokens.add(token);
        }
    }

    public void removeFcmToken(String token) {
        fcmTokens.remove(token);
    }
}