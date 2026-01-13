package com.project.backend.domain.member.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum MemberErrorCode implements BaseErrorCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404", "회원을 찾을 수 없습니다"),
    MEMBER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "MEMBER400_1", "이미 탈퇴한 회원입니다"),
    MEMBER_WITHDRAWN_REJOIN_RESTRICTED(HttpStatus.FORBIDDEN, "MEMBER403_1", "탈퇴 후 3개월간 재가입이 불가능합니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
