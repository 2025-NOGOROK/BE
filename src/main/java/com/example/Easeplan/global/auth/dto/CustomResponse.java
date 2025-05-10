package com.example.Easeplan.global.auth.dto;

public class CustomResponse<T> {
    private String message;
    private T data;

    public CustomResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }

    // Getter
    public String getMessage() { return message; }
    public T getData() { return data; }
}
