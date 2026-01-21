package com.project.backend.domain.nlp.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NlpErrorCode implements BaseErrorCode {

    LLM_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "NL001", "AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요."),
    LLM_PARSE_ERROR(HttpStatus.UNPROCESSABLE_ENTITY, "NL002", "입력을 이해하지 못했어요. 다시 입력해주세요."),
    LLM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "NL003", "AI 응답 시간이 초과되었습니다. 다시 시도해주세요."),

    INVALID_ITEM_TYPE(HttpStatus.BAD_REQUEST, "NL101", "저장할 수 없는 타입입니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "NL102", "날짜 형식이 올바르지 않습니다."),
    INVALID_TIME_FORMAT(HttpStatus.BAD_REQUEST, "NL103", "시간 형식이 올바르지 않습니다."),

    INVALID_RECURRENCE_RULE(HttpStatus.BAD_REQUEST, "NL201", "반복 규칙이 올바르지 않습니다."),
    RECURRENCE_DATE_EXCEEDED(HttpStatus.BAD_REQUEST, "NL202", "반복 일정 생성 기간이 너무 깁니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
