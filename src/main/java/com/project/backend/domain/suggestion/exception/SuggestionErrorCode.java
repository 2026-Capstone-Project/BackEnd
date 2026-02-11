package com.project.backend.domain.suggestion.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuggestionErrorCode implements BaseErrorCode {
    SUGGESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUG404", "존재하지 않는 제안입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
