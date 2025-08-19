package com.example.Easeplan.global.exception;

import com.example.Easeplan.api.Calendar.service.GoogleOAuthService;
import com.example.Easeplan.global.common.ErrorCode;
import com.example.Easeplan.global.dto.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private final GoogleOAuthService googleOAuthService;

    public GlobalExceptionHandler(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    public static class GoogleRelinkRequiredException extends RuntimeException {
        public GoogleRelinkRequiredException() { super("Google account needs to be relinked."); }
        public GoogleRelinkRequiredException(String message) { super(message); }
        public GoogleRelinkRequiredException(Throwable cause) { super("Google account needs to be relinked.", cause); }
        public GoogleRelinkRequiredException(String message, Throwable cause) { super(message, cause); }
    }

    @ExceptionHandler(GoogleRelinkRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleGoogleRelinkRequired(GoogleRelinkRequiredException e) {
        ErrorCode code = ErrorCode.GOOGLE_RELINK_REQUIRED;
        return ResponseEntity.status(code.getStatus()).body(
                new ApiErrorResponse(
                        code.name(),
                        e.getMessage() != null ? e.getMessage() : code.getDefaultMessage(),
                        googleOAuthService.buildAuthUrl()
                )
        );
    }


}

