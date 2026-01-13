package com.project.backend.domain.auth.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
    // ErrorCode

    // 공통
    INVALID_OAUTH_REQUEST(HttpStatus.BAD_REQUEST, "AUTH400", "잘못된 OAuth 요청"),
    OAUTH_STATE_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH401", "세션 검증에 실패"),

    // NAVER
    NAVER_USER_INFO_NOT_FOUND(HttpStatus.UNAUTHORIZED, "NAVER401", "네이버 유저 정보가 존재하지 않음"),
    NAVER_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "NAVER401", "네이버 토큰 정보가 존재하지 않음"),
    NAVER_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "NAVER500", "네이버 인증 서버 오류입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
