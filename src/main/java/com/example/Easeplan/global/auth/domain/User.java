package com.example.Easeplan.global.auth.domain;

import com.example.Easeplan.api.Calendar.domain.GoogleCalendarInfo;
import com.example.Easeplan.api.SmartWatch.domain.SmartwatchData;
import com.example.Easeplan.api.Survey.domain.UserSurvey;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(name = "`user`")
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String name; // 추가

    @Column(nullable = false)
    private String birth; // 추가 (Date 타입으로 저장하고 싶으면 LocalDate로 변경 가능)

    @Column
    private String gender; // 선택


    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean pushNotificationAgreed; // 푸시 알림 동의 여부

    @Column
    private String deviceToken; //푸시 알림을 보내기 위해 반드시 필요한 기기 고유 식별자


    // 약관 동의 필드 추가
    @Column(nullable = false)
    private boolean termsOfServiceAgreed;

    @Column(nullable = false)
    private boolean privacyPolicyAgreed;

    @Column(nullable = false)
    private boolean healthInfoPolicyAgreed;

    @Column(nullable = false)
    private boolean locationPolicyAgreed;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserSurvey userSurvey;

    @Column(name = "google_access_token")
    private String googleAccessToken;

    @Column(name = "google_refresh_token")
    private String googleRefreshToken;

    // 액세스 토큰 업데이트 메서드
    public void updateGoogleTokens(String accessToken, String refreshToken) {
        this.googleAccessToken = accessToken;
        this.googleRefreshToken = refreshToken;
    }

    // 추가
    public void setPassword(String password) {
        this.password = password;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ✅ "ROLE_USER" 권한 추가
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email; // UserDetails의 username은 email로 매핑
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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<GoogleCalendarInfo> calendarEvents = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SmartwatchData> smartwatchData = new ArrayList<>();
}
