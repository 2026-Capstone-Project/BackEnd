package com.project.backend.domain.setting.exception;

import com.project.backend.global.apiPayload.exception.CustomException;

public class SettingException extends CustomException {
    public SettingException(SettingErrorCode errorCode) {
        super(errorCode);
    }
}
