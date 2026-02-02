package com.project.backend.domain.event.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum RecurrenceGroupErrorCode implements BaseErrorCode {

    INVALID_END_CONDITION
            (HttpStatus.BAD_REQUEST, "RG400_1", "EndType 타입에 따른 불필요한 필드값이 채워져 있습니다."),
    END_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "RG400_2", "종료 날짜가 설정되지 않았습니다."),
    END_COUNT_REQUIRED(HttpStatus.BAD_REQUEST, "RG400_3", "종료 카운트가 설정되지 않았습니다."),
    INVALID_END_TYPE(HttpStatus.BAD_REQUEST, "RG400_4", "잘못된 종료타입입니다."),
    DAYS_OF_WEEK_REQUIRED(HttpStatus.BAD_REQUEST, "RG400_5", "매주 무슨 요일 반복인지 설정되지 않았습니다."),
    MONTHLY_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "RG400_6", "매달 반복 타입이 설정되지 않았습니다."),
    DAY_OF_MONTH_REQUIRED(HttpStatus.BAD_REQUEST, "RG400_7", "매달 반복 일이 설정되지 않았습니다."),
    WEEK_OF_MONTH_REQUIRED(HttpStatus.BAD_REQUEST, "RG400_8", "매달 반복 주가 설정되지 않았습니다."),
    DAY_OF_WEEK_IN_MONTH_REQUIRED
            (HttpStatus.BAD_REQUEST, "RG400_9", "그 달의 n번째주 요일이 설정되지 않았습니다."),
    INVALID_FREQUENCY_TYPE(HttpStatus.BAD_REQUEST, "RG400_11", "잘못된 반복 타입입니다."),
    MONTH_OF_YEAR_REQUIRED(HttpStatus.BAD_REQUEST, "RG400_12", "매년 반복 월이 설정되지 않았습니다."),
    INVALID_END_DATE_RANGE(HttpStatus.BAD_REQUEST, "RG400_13", "종료 날짜가 일정 시작 날짜보다 빠릅니다."),
    INVALID_DAY_OF_WEEK(HttpStatus.BAD_REQUEST, "RG400_14", "잘못된 요일입니다."),
    INVALID_FREQUENCY_CONDITION
            (HttpStatus.BAD_REQUEST, "RG400_15", "FREQUENCY 타입에 따른 불필요한 필드값이 채워져 있습니다."),
    INVALID_DAILY_INTERVAL_VALUE
            (HttpStatus.BAD_REQUEST, "RG400_17", "n일 간격 범위가 올바르지 않습니다.(1~364)"),
    INVALID_WEEKLY_INTERVAL_VALUE
            (HttpStatus.BAD_REQUEST, "RG400_17", "매주 간격 범위가 올바르지 않습니다.(1)"),
    INVALID_MONTHLY_INTERVAL_VALUE
            (HttpStatus.BAD_REQUEST, "RG400_17", "n월 간격 범위가 올바르지 않습니다.(1~11)"),
    INVALID_YEARLY_INTERVAL_VALUE
            (HttpStatus.BAD_REQUEST, "RG400_17", "n년 간격 범위가 올바르지 않습니다.(1~99)"),




    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
