package com.example.Easeplan.api.SmartWatch.domain;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "smartwatch_data",
        indexes = @Index(name = "idx_user_device", columnList = "user_email, device_id"))
public class SmartwatchData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", referencedColumnName = "email")
    private User user;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private Double stressIndex;  // 0.0 ~ 100.0 스케일 [3][6]

    @Column(nullable = false)
    private LocalDateTime measuredAt;  // 측정 시간

    // 추가 가능한 필드 (선택사항)
    private Integer heartRate;
    private Double temperature;


}


