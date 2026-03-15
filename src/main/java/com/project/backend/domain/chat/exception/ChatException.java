package com.project.backend.domain.chat.exception;

import com.project.backend.domain.auth.exception.AuthErrorCode;
import com.project.backend.global.apiPayload.exception.CustomException;

public class ChatException extends CustomException {
    public ChatException(ChatErrorCode errorCode) {
        super(errorCode);
    }
}
