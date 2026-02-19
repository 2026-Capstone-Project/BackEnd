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
    DAYS_OF_WEEK_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT404_4", "WEEKLY 반복의 요일 정보를 찾을 수 없습니다"),
    INVALID_MONTHLY_TYPE
            (HttpStatus.INTERNAL_SERVER_ERROR, "EVENT500_5", "MONTHLY_TYPE이 존재하지 않습니다"),
    INVALID_RECURRENCE_FREQUENCY
            (HttpStatus.INTERNAL_SERVER_ERROR, "EVENT500_6", "FREQUENCY가 존재하지 않습니다"),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT404_1", "일정을 찾을 수 없습니다"),
    UPDATE_SCOPE_NOT_REQUIRED(HttpStatus.BAD_REQUEST, "EVENT400_3", "UPDATE_SCOPE 설정이 필요하지 않습니다."),
    OCCURRENCE_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "EVENT400_5", "OCCURRENCE_DATE가 없습니다."),
    UPDATE_SCOPE_REQUIRED(HttpStatus.BAD_REQUEST, "EVENT400_7", "UPDATE_SCOPE가 없습니다."),
    INVALID_UPDATE_SCOPE(HttpStatus.BAD_REQUEST, "EVENT400_8", "존재하지 않는UPDATE_SCOPE 값입니다."),
    NOT_RECURRING_EVENT(HttpStatus.BAD_REQUEST, "EVENT400_9", "단일 일정입니다.")
    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
