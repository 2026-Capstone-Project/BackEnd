package com.project.backend.domain.auth.kakao;

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
@RequiredArgsConstructor
@RequestMapping("/api/v1/")
public class KakaoController implements KakaoDocs {

    private final KakaoService kakaoService;

    @Override
    @GetMapping("/auth/kakao")
    public void redirectToKakao(
            HttpServletResponse response,
            HttpSession session
    ) throws IOException {
        kakaoService.redirectToKakao(response, session);
    }

    @Override
    @GetMapping("/auth/kakao/callback")
    public CustomResponse<String> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response,
            HttpSession session
    ) {
        kakaoService.callback(code, state, response, session);
        return CustomResponse.onSuccess("OK", "카카오 로그인 성공");
    }
}
