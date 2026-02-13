package com.project.backend.domain.briefing.controller;

import com.project.backend.domain.briefing.dto.response.BriefingResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "브리핑 API", description = "사용자 하루 요약 브리핑 조회 API")
public interface BriefingDocs {

    @Operation(
            summary = "오늘의 브리핑 조회",
            description = """
                    로그인한 사용자의 오늘 브리핑 정보를 조회합니다.
                    
                    - 조회 시점 기준으로 계산된 최신 브리핑 결과를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "오늘의 브리핑 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = BriefingResDTO.BriefingRes.class
                            )
                    )
            )
    })
    CustomResponse<BriefingResDTO.BriefingRes> getBriefing(
            CustomUserDetails customUserDetails
    );
}