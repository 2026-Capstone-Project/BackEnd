package com.project.backend.domain.friend.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum FriendErrorCode implements BaseErrorCode {

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
