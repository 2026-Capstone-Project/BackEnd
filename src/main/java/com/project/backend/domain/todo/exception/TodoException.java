package com.project.backend.domain.todo.exception;

import com.project.backend.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class TodoException extends CustomException {

    public TodoException(TodoErrorCode errorCode) {
        super(errorCode);
    }
}
