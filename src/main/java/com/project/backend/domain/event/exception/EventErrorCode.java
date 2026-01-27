package com.project.backend.domain.event.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum EventErrorCode implements BaseErrorCode {

    INVALID_TIME(HttpStatus.BAD_REQUEST, "EVENT400_1", "시간을 설정하지 않았습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "EVENT400_2", "시간 설정이 잘못되었습니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT404", "일정을 찾을 수 없습니다")
    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
