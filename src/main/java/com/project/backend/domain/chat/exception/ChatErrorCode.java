package com.project.backend.domain.chat.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    CHAT_API_ERROR(HttpStatus.BAD_GATEWAY, "CHAT_502", "AI 응답 생성에 실패했습니다."),
    EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "CHAT_400", "메시지가 비어 있습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
