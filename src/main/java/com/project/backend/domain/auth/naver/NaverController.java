package com.project.backend.domain.auth.naver;

import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/")
@Tag(name = "Naver", description = "네이버 OAuth")
public class NaverController {

    private final NaverService naverService;

    @GetMapping("/auth/naver")
    public CustomResponse<String> redirectToNaver(
            HttpServletResponse response,
            HttpSession session
    ) throws IOException {
        naverService.redirectToNaver(response, session);
        return CustomResponse.onSuccess("Redirect", "리디렉션 완료");
    }

    @GetMapping("/auth/naver/callback")
    public CustomResponse<String> callback(
            HttpServletRequest request
    ) {
        return CustomResponse.onSuccess("네이버 소셜 인증 완료", naverService.callback(request));
    }
}
