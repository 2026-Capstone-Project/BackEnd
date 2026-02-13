package com.project.backend.domain.reminder.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReminderErrorCode implements BaseErrorCode {
    REMINDER_NOT_FOUND
            (HttpStatus.BAD_REQUEST, "TODO400_1", "리마인더를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
