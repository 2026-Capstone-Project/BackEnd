package com.project.backend.domain.member.controller.docs;

import com.project.backend.domain.member.dto.response.MemberResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Member API", description = "회원 관련 API")
public interface MemberDocs {

    @Operation(
            summary = "내 정보 조회",
            description = """
                    현재 로그인한 사용자의 기본 정보를 조회합니다.

                    **응답 필드:**
                    - memberId: 회원 고유 ID
                    - nickname: 사용자 이름
                    - email: 이메일 주소
                    - provider: 소셜 로그인 수단 (KAKAO, NAVER, GOOGLE)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "200",
                                    "message": "요청에 성공하였습니다.",
                                    "result": {
                                        "memberId": 1,
                                        "nickname": "홍길동",
                                        "email": "hong@example.com",
                                        "provider": "KAKAO"
                                    }
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "COMMON401",
                                    "message": "인증이 필요합니다."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "MEMBER404",
                                    "message": "회원을 찾을 수 없습니다."
                                }
                                """)
                    )
            )
    })
    CustomResponse<MemberResDTO.MyInfo> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    로그인한 회원의 계정을 탈퇴 처리합니다.

                    **처리 내용:**
                    - Event, Todo, Suggestion, Setting: 즉시 삭제
                    - Member: Soft Delete (3개월 후 자동 Hard Delete)
                    - Auth: 유지 (재가입 방지용, 3개월 후 삭제)
                    - Access/Refresh Token 무효화

                    **주의:** 동일 소셜 계정으로 3개월간 재가입이 불가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": true,
                                    "code": "200",
                                    "message": "회원 탈퇴가 완료되었습니다.",
                                    "result": null
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "COMMON401",
                                    "message": "인증이 필요합니다."
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                    "isSuccess": false,
                                    "code": "MEMBER404",
                                    "message": "회원을 찾을 수 없습니다."
                                }
                                """)
                    )
            )
    })
    CustomResponse<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response
    );
}
