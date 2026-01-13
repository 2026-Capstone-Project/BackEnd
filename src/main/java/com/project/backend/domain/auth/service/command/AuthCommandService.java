package com.project.backend.domain.auth.service.command;

import com.project.backend.domain.auth.dto.response.AuthResDTO;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthCommandService {

    void loginOrSignup(HttpServletResponse response, AuthResDTO.UserAuth userAuth);

}
