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

    // KAKAO
    KAKAO_INVALID_STATE(HttpStatus.BAD_REQUEST, "KAKAO_001", "유효하지 않은 요청입니다."),
    KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_002", "카카오 소셜 로그인 토큰 발급에 실패했습니다."),
    KAKAO_USER_INFO_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_003", "카카오 소셜 로그인 사용자 정보 조회에 실패했습니다."),

    // GOOGLE
    GOOGLE_TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "GOOGLE_001", "구글 소셜 로그인 토큰 발급에 실패했습니다."),
    GOOGLE_ID_TOKEN_DECODE_FAILED(HttpStatus.BAD_REQUEST, "GOOGLE_002", "구글 ID 토큰 디코딩에 실패했습니다."),
    GOOGLE_ISS_VALIDATION_FAILED(HttpStatus.UNAUTHORIZED, "GOOGLE_003", "구글 토큰 발급자(iss) 검증에 실패했습니다."),
    GOOGLE_AUD_VALIDATION_FAILED(HttpStatus.UNAUTHORIZED, "GOOGLE_004", "구글 토큰 대상자(aud) 검증에 실패했습니다."),
    GOOGLE_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "GOOGLE_005", "구글 토큰이 만료되었습니다."),

    // 공통 - OAuth 통합
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH402", "지원하지 않는 OAuth Provider입니다."),

    // 재가입 제한
    REJOIN_RESTRICTED(HttpStatus.FORBIDDEN, "AUTH403", "탈퇴후3개월간재가입이제한됩니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
