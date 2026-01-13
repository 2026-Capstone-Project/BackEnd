package com.project.backend.domain.member.exception;

import com.project.backend.domain.auth.entity.Auth;
import com.project.backend.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class MemberException extends CustomException {

    public MemberException(MemberErrorCode errorCode){
        super(errorCode);
    }
}
