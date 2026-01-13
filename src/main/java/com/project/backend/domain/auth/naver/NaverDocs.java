package com.project.backend.domain.auth.naver;

import com.project.backend.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Tag(name = "Naver", description = "네이버 OAuth")
public interface NaverDocs {

    @Operation(summary = "네이버 OAuth 콜백",
            description = """
        네이버 로그인 인증 후 호출되는 콜백 API입니다.<br>
        <br>
        처리 흐름:
        <ol>
          <li>state 값 검증</li>
          <li>네이버 Access Token 발급</li>
          <li>네이버 사용자 정보 조회</li>
          <li>회원가입 또는 로그인 처리</li>
          <li>JWT 쿠키 발급</li>
        </ol>
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "네이버 로그인 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 OAuth 요청 (AUTH400)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
            인증 실패<br>
            - 세션 검증 실패 (AUTH401)<br>
            - 네이버 토큰 없음 (NAVER401)<br>
            - 네이버 유저 정보 없음 (NAVER401)
            """
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "네이버 인증 서버 오류 (NAVER500)"
            )
    })
    void redirectToNaver(
            HttpServletResponse response,
            HttpSession session
    ) throws IOException;

    @Operation(summary = "네이버 서버가 사용하는 api", description = "프론트는 호출하지 않음")
    CustomResponse<String> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response,
            HttpSession session
    );
}
