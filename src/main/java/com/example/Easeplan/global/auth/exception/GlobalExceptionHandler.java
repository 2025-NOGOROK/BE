package com.example.Easeplan.global.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 내부 예외 클래스
    public static class GoogleRelinkRequiredException extends RuntimeException {
        public GoogleRelinkRequiredException() {
            super("Google account needs to be relinked.");
        }
        public GoogleRelinkRequiredException(Throwable cause) {
            super("Google account needs to be relinked.", cause);
        }
    }

    @ExceptionHandler(GoogleRelinkRequiredException.class)
    public ResponseEntity<String> handleGoogleRelinkRequired(GoogleRelinkRequiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }
}
