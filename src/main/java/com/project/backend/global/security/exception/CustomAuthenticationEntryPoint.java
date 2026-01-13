package com.project.backend.global.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.global.apiPayload.CustomResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            HttpServletResponse response,
            @NonNull AuthenticationException authException
    ) throws IOException {

        String code;
        String message;

        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(401);

        if (authException instanceof BadCredentialsException ex) {
            code = SecurityErrorCode.UNAUTHORIZED.getCode();
            message = ex.getMessage();
        } else {
            code = SecurityErrorCode.UNAUTHORIZED.getCode();
            message = SecurityErrorCode.UNAUTHORIZED.getMessage();
        }
        CustomResponse<Object> unauthorizedErrorResponse = CustomResponse.onFailure(code, message, null);
        new ObjectMapper().writeValue(response.getWriter(), unauthorizedErrorResponse);
    }
}
