package com.example.Easeplan.api.SmartWatch.domain;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Setter
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
    private LocalDateTime measuredAt;  // 측정 시간

    // 생체 데이터
    private Float min;
    private Float max;
    private Float avg;
    private Float stress;
    private Integer heartRate;
    private Integer totalMinutes;
    private Float bloodOxygen;
    private Float skinTemperature;

    private String startTime;
    private String endTime;
}
