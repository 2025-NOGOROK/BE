package com.example.Easeplan.global.auth.dto;

import lombok.Getter;
import lombok.Setter;



public record SignInRequest(
        String email,
        String password
) {

}
