package com.project.backend.domain.nlp.exception;

import com.project.backend.global.apiPayload.exception.CustomException;

public class NlpException extends CustomException {
    public NlpException(NlpErrorCode errorCode) {
        super(errorCode);
    }
}
