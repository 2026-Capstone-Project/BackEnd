package com.project.backend.domain.auth.service.command;

import com.project.backend.domain.auth.dto.response.AuthResDTO;
public interface AuthCommandService {

    void loginOrSignup(AuthResDTO.UserAuth userAuth);

}
