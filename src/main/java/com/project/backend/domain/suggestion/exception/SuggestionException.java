package com.project.backend.domain.suggestion.exception;

import com.project.backend.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class SuggestionException extends CustomException {
    public SuggestionException(SuggestionErrorCode errorCode) {
        super(errorCode);
    }
}
