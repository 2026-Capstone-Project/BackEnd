package com.project.backend.domain.todo.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum TodoErrorCode implements BaseErrorCode {

    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "TODO404_1", "할 일을 찾을 수 없습니다."),
    TODO_FORBIDDEN(HttpStatus.FORBIDDEN, "TODO403_1", "해당 할 일에 대한 권한이 없습니다."),
    INVALID_RECURRENCE_SETTING(HttpStatus.BAD_REQUEST, "TODO400_1", "잘못된 반복 설정입니다."),
    INVALID_UPDATE_SCOPE(HttpStatus.BAD_REQUEST, "TODO400_2", "잘못된 수정 범위입니다."),
    OCCURRENCE_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "TODO400_3", "반복 할 일의 경우 occurrenceDate가 필요합니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
