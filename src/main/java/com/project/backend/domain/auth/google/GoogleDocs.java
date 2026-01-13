package com.project.backend.domain.auth.google;

import com.project.backend.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Tag(name = "GoogleAuth API", description = "소셜 로그인/ 인증 API")
public interface GoogleDocs {

    @Operation(
            summary = "구글 로그인 요청",
            description = "구글 OAuth 로그인 페이지로 리다이렉트한다."
    )
    void redirectToGoogle(HttpServletResponse response, HttpSession session) throws IOException;

    @Operation(
            summary = "구글 로그인 콜백",
            description = "구글 OAuth 인증 후 콜백 처리해서 자동 로그인/회원가입 실행"
    )
    CustomResponse<String> googleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response,
            HttpSession session
    );
}
