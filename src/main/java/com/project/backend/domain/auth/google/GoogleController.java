package com.project.backend.domain.auth.google;

import com.project.backend.global.apiPayload.CustomResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
public class GoogleController implements GoogleDocs{

    private final GoogleAuthService googleAuthService;

    @Override
    @GetMapping("")
    public void redirectToGoogle(HttpServletResponse response, HttpSession session) throws IOException {
        String url = googleAuthService.generateAuthorizationUrl(session);
        response.sendRedirect(url);
    }

    @Override
    @GetMapping("/callback")
    public CustomResponse<String> googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response,
            HttpSession session
    ) {
        googleAuthService.exchangeCode(code, state, response, session);
        return CustomResponse.onSuccess("Created", "로그인 성공");
    }
}
