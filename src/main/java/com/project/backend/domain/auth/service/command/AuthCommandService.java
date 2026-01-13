package com.project.backend.domain.auth.service.command;

import com.project.backend.domain.auth.dto.response.AuthResDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthCommandService {

    void loginOrSignup(HttpServletRequest request, HttpServletResponse response, AuthResDTO.UserAuth userAuth);

}
