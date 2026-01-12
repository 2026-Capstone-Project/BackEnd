package com.project.backend.global.security.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SecurityErrorCode implements BaseErrorCode {
    // ErrorCode
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "접근이 금지되었습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "요청한 자원을 찾을 수 없습니다"),

    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "TOKEN401_2", "Refresh Token이 만료되었습니다"),
    REQUIRED_RE_LOGIN(HttpStatus.UNAUTHORIZED, "TOKEN401", "모든 토큰이 만료되었습니다. 다시 로그인 하세요"),

    MISSING_CSRF_TOKEN(HttpStatus.FORBIDDEN, "CSRF403_1", "CSRF 토큰이 누락되었습니다."),
    INVALID_CSRF_TOKEN(HttpStatus.FORBIDDEN, "CSRF403_2", "CSRF 토큰이 유효하지 않습니다."),

    ROLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ROLE403", "관리자 권한이 필요합니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}