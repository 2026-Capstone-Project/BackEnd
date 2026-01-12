package com.project.backend.domain.auth.google;

import com.project.backend.global.apiPayload.CustomResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class GoogleController {

    private final GoogleAuthService googleAuthService;

    @GetMapping("/api/v1/auth/google")
    public void redirectToGoogle(HttpServletResponse response, HttpSession session) throws IOException {
        String url = googleAuthService.generateAuthorizationUrl(session);
        response.sendRedirect(url);
    }

    @GetMapping("/api/v1/auth/google/callback")
    public CustomResponse<String> googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpSession session
    ) {
        googleAuthService.exchangeCode(code, state, session);
        return CustomResponse.onSuccess("Created", "로그인 성공");
    }
}
