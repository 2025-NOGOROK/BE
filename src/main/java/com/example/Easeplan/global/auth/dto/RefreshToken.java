package com.example.Easeplan.global.auth.dto;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column
    private String token;

//    @ManyToOne
//    @JoinColumn(name = "email", insertable = false, updatable = false)
//    private User user;

    public RefreshToken() {}

    @Builder
    public RefreshToken(String email, String token) {
        this.email = email;
        this.token = token;
//        this.user = user;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
