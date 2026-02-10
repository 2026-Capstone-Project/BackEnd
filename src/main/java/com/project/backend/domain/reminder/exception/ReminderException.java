package com.project.backend.domain.reminder.exception;

import com.project.backend.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class ReminderException extends CustomException {
    public ReminderException(ReminderErrorCode errorCode) {
        super(errorCode);
    }
}
