package com.project.backend.domain.auth.kakao;

import com.project.backend.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Tag(name = "Kakao", description = "카카오 OAuth")
public interface KakaoDocs {

    @Operation(summary = "카카오 로그인 페이지로 리다이렉트",
            description = """
        카카오 OAuth 로그인을 시작합니다.<br>
        이 API를 호출하면 카카오 로그인 페이지로 리다이렉트됩니다.
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "카카오 로그인 페이지로 리다이렉트"
            )
    })
    void redirectToKakao(
            HttpServletResponse response,
            HttpSession session
    ) throws IOException;

    @Operation(summary = "카카오 OAuth 콜백",
            description = """
        카카오 로그인 인증 후 호출되는 콜백 API입니다.<br>
        <br>
        처리 흐름:
        <ol>
          <li>state 값 검증 (CSRF 방지)</li>
          <li>카카오 Access Token 발급</li>
          <li>카카오 사용자 정보 조회</li>
          <li>회원가입 또는 로그인 처리</li>
          <li>JWT 쿠키 발급</li>
        </ol>
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "카카오 로그인 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 OAuth 요청 (AUTH400)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
            인증 실패<br>
            - 세션 검증 실패 (AUTH401)
            """
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = """
            카카오 인증 서버 오류<br>
            - 토큰 발급 실패 (KAKAO_002)<br>
            - 사용자 정보 조회 실패 (KAKAO_003)
            """
            )
    })
    CustomResponse<String> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response,
            HttpSession session
    );
}
