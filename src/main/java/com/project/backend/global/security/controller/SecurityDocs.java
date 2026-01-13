package com.project.backend.global.security.controller;

import com.project.backend.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "Security", description = "Security API")
public interface SecurityDocs {

    @Operation(
            summary = "Access Token 재발급 (쿠키)",
            description = """
                Refresh Token 쿠키가 유효한 경우 Access Token 쿠키를 재발급합니다.<br>
                <b>조건</b>
                <ul>
                    <li>Refresh Token 쿠키 존재</li>
                    <li>Refresh Token 만료되지 않음</li>
                    <li>Redis 서버에 Refresh Token 존재</li>
                </ul>
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Access Token 재발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = """
                                {
                                  "isSuccess": true,
                                  "code": "200",
                                  "message": "엑세스 쿠키가 재발급 되었습니다.",
                                  "result": "OK"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "모든 토큰 만료 (재로그인 필요)",
                    content = @Content(
                            schema = @Schema(example = """
                                {
                                  "isSuccess": false,
                                  "code": "TOKEN401",
                                  "message": "모든 토큰이 만료되었습니다. 다시 로그인 하세요"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh Token 만료",
                    content = @Content(
                            schema = @Schema(example = """
                                {
                                  "isSuccess": false,
                                  "code": "TOKEN401_2",
                                  "message": "Refresh Token이 만료되었습니다"
                                }
                                """)
                    )
            )
    })
    @PostMapping("/reissue-cookie")
    CustomResponse<String> reissueCookie(
            HttpServletRequest request,
            HttpServletResponse response
    );


    @Operation(
            summary = "CSRF 토큰 발급",
            description = """
                CSRF 토큰을 생성하여 쿠키로 발급합니다.<br>
                <b>설명</b>
                <ul>
                    <li>Spring Security CsrfToken을 lazy 방식으로 생성</li>
                    <li>쿠키에 자동으로 Set-Cookie 처리</li>
                </ul>
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "CSRF 토큰 발급 성공",
                    content = @Content(
                            schema = @Schema(example = """
                                {
                                  "isSuccess": true,
                                  "code": "200",
                                  "message": "CSRF 토큰이 쿠키로 발급되었습니다.",
                                  "result": "OK"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF 토큰 누락 또는 검증 실패",
                    content = @Content(
                            schema = @Schema(example = """
                                {
                                  "isSuccess": false,
                                  "code": "CSRF403_1",
                                  "message": "CSRF 토큰이 누락되었습니다."
                                }
                                """)
                    )
            )
    })
    @GetMapping("/csrf")
    CustomResponse<String> csrf(
            HttpServletRequest request,
            HttpServletResponse response
    );
}
