package com.project.backend.domain.notification.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "SET404", "Setting이 존재하지 않습니다"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
