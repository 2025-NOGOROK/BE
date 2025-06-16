package com.example.Easeplan.global.auth.dto;

public class SignInRequest {
    private String email;
    private String password;

    // 생성자
    public SignInRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getter 추가
    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
