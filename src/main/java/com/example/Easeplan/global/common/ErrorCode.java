package com.example.Easeplan.global.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    GOOGLE_RELINK_REQUIRED(HttpStatus.UNAUTHORIZED, "Google account needs to be relinked."),
    INVALID_CODE(HttpStatus.UNAUTHORIZED, "Invalid or expired authorization code."),
    BAD_REQUEST_VALIDATION(HttpStatus.BAD_REQUEST, "Validation failed."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found."),
    CONFLICT(HttpStatus.CONFLICT, "Conflict."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Rate limited."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
