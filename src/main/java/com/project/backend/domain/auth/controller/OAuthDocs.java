package com.project.backend.domain.auth.controller;

import com.project.backend.domain.auth.enums.Provider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@Tag(name = "OAuth", description = "소셜 로그인 통합 API")
public interface OAuthDocs {

    @Operation(
            summary = "소셜 로그인 페이지로 리다이렉트",
            description = """
                지정된 OAuth Provider의 로그인 페이지로 리다이렉트합니다.

                **지원 Provider:**
                - kakao: 카카오 로그인
                - naver: 네이버 로그인
                - google: 구글 로그인
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "OAuth 로그인 페이지로 리다이렉트"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 Provider")
    })
    void redirectToProvider(
            @Parameter(description = "OAuth Provider (kakao, naver, google)", required = true)
            Provider provider,
            HttpServletResponse response,
            HttpSession session
    ) throws IOException;

    @Operation(
            summary = "OAuth 콜백 처리",
            description = """
                OAuth Provider 인증 후 호출되는 콜백 API입니다.

                **처리 흐름:**
                1. state 값 검증 (CSRF 방지)
                2. Access Token / ID Token 발급
                3. 사용자 정보 조회
                4. 재가입 제한 검증 (탈퇴 후 3개월 이내 재가입 불가)
                5. 회원가입 또는 로그인 처리
                6. JWT 쿠키 발급
                7. 프론트엔드로 리다이렉트

                **에러 발생 시:**
                - 프론트엔드 로그인 페이지로 리다이렉트
                - 쿼리 파라미터: `?error={에러코드}&message={에러메시지}`
                - 예시: `/login?error=AUTH403&message=탈퇴 후 3개월간 재가입이 제한됩니다.`

                **주요 에러코드:**
                - `AUTH403`: 재가입 제한 (탈퇴 후 3개월 이내)
                - `AUTH401`: 세션 검증 실패
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "로그인 성공 - 프론트엔드 홈으로 리다이렉트"),
            @ApiResponse(responseCode = "302", description = "로그인 실패 - 프론트엔드 로그인 페이지로 리다이렉트 (에러 파라미터 포함)")
    })
    void handleCallback(
            @Parameter(description = "OAuth Provider (kakao, naver, google)", required = true)
            Provider provider,
            @Parameter(description = "OAuth 인가 코드", required = true)
            String code,
            @Parameter(description = "CSRF 방지용 state 값", required = true)
            String state,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) throws IOException;
}
