package com.example.Easeplan.api.SmartWatch.domain;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "heart_rate")
public class HeartRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", referencedColumnName = "email")
    private User user;

    private Float min;
    private Float max;
    private Float avg;
    private String startTime;
    private String endTime;
    private Integer count;
    private Float stress;
}
