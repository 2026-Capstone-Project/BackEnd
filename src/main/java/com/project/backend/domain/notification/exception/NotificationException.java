package com.project.backend.domain.notification.exception;

import com.project.backend.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class NotificationException extends CustomException {
    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }
}
