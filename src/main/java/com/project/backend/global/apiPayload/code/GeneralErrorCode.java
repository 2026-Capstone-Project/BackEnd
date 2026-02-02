package com.project.backend.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum GeneralErrorCode implements BaseErrorCode{

    BAD_REQUEST_400(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다"),

    UNAUTHORIZED_401(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다"),

    FORBIDDEN_403(HttpStatus.FORBIDDEN, "COMMON403", "접근이 금지되었습니다"),

    NOT_FOUND_404(HttpStatus.NOT_FOUND, "COMMON404", "요청한 자원을 찾을 수 없습니다"),

    INTERNAL_SERVER_ERROR_500(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다"),

    // 유효성 검사
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALID400_0", "잘못된 파라미터 입니다."),
    DTO_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALID400_1", "잘못된 DTO 필드입니다."),

//    INVALID_JSON_SYNTAX(HttpStatus.BAD_REQUEST, "VALID400_1", "요청 본문의 JSON 문법이 잘못됨 / e : JsonParseException"),
    INVALID_FIELD_FORMAT(HttpStatus.BAD_REQUEST, "VALID400_2","필드 값의 형식이 올바르지 않음 / e : InvalidFormatException"),
    BAD_REQUEST_BODY(HttpStatus.BAD_REQUEST, "VALID400_3", "요청 본문을 읽을 수 없음, JSON 문법 오류일 가능성"),

    INVALID_ENUM(HttpStatus.BAD_REQUEST, "VALID400_4", "ENUM 입력 오류"),
    INVALID_LOCAL_TIME(HttpStatus.BAD_REQUEST, "LOCAL_TIME400", "HH:mm 형식의 유효한 시간이 아닙니다 (00:00 ~ 23:59 범위만 허용)"),

    ;

    // 필요한 필드값 선언
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
