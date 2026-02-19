package com.project.backend.domain.suggestion.controller.docs;

import com.project.backend.domain.suggestion.dto.response.SuggestionResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "선제적 제안 API", description = "선제적 제안 관련 API")
public interface SuggestionDocs {

    @Operation(
            summary = "선제적 제안 목록 조회",
            description = """
                    로그인한 사용자의 선제적 제안 목록을 조회합니다.

                    - 사용자에게 노출 가능한 제안 리스트를 반환합니다.
                    - 제안이 없는 경우에도 정상 응답을 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "선제적 제안 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuggestionResDTO.SuggestionListRes.class)
                    )
            )
    })
    CustomResponse<SuggestionResDTO.SuggestionListRes> getSuggestions(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "선제적 제안 생성 (프론트 개발용 임시 API)",
            description = """
                    프론트 개발 편의를 위해 제공되는 임시 API입니다.

                    - 로그인한 사용자의 선제적 제안을 서버 로직에 따라 생성합니다.
                    - 운영에서는 사용하지 않는 것을 권장합니다.
                    - 응답 본문 데이터는 없습니다(null).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "선제적 제안 생성 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class)
                    )
            )
    })
    CustomResponse<String> createSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "선제적 제안 수락",
            description = """
                    특정 선제적 제안을 수락 처리합니다.

                    - suggestionId에 해당하는 제안을 수락합니다.
                    - 이미 처리된 제안이거나 존재하지 않는 경우 404를 반환합니다.
                    - 동시에 수락 요청이 들어와 경합이 발생하면 409를 반환합니다.
                    - 응답 본문 데이터는 없습니다(null).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "선제적 제안 수락 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "이미 처리되었거나, 존재하지 않는 제안",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "SUGGESTION_NOT_FOUND",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "SUG404",
                                              "message": "이미 처리되었거나, 존재하지 않는 제안입니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 진행중인 수락(동시 수락 충돌)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "SUGGESTION_CONFLICT",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "SUG409",
                                              "message": "이미 진행중인 수락입니다."
                                            }
                                            """
                            )
                    )
            )
    })
    CustomResponse<String> acceptSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable("suggestionId") Long suggestionId
    );

    @Operation(
            summary = "선제적 제안 거절",
            description = """
                    특정 선제적 제안을 거절 처리합니다.

                    [상태 전이 규칙]
                    - 현재 상태가 PRIMARY이고 secondary가 존재하면:
                      - PRIMARY는 거절 처리되고, 사용자에게는 SECONDARY 제안이 이어서 노출됩니다(상태가 SECONDARY로 전환).
                    - 현재 상태가 PRIMARY인데 secondary가 없으면:
                      - 해당 제안은 최종 REJECTED 처리됩니다.
                    - 현재 상태가 SECONDARY인 제안을 거절하면:
                      - 해당 제안은 최종 REJECTED 처리됩니다.

                    - 이미 처리된 제안이거나 존재하지 않는 경우 404를 반환합니다.
                    - 응답 본문 데이터는 없습니다(null).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "선제적 제안 거절 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "이미 처리되었거나, 존재하지 않는 제안",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "SUGGESTION_NOT_FOUND",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "SUG404",
                                              "message": "이미 처리되었거나, 존재하지 않는 제안입니다."
                                            }
                                            """
                            )
                    )
            )
    })
    CustomResponse<String> rejectSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable("suggestionId") Long suggestionId
    );

    @Operation(
            summary = "선제적 제안 전체 삭제 (프론트 개발용 임시 API)",
            description = """
                    프론트 개발 편의를 위해 제공되는 임시 API입니다.

                    - 로그인한 사용자의 선제적 제안을 모두 삭제합니다.
                    - 운영에서는 사용하지 않는 것을 권장합니다.
                    - 응답 본문 데이터는 없습니다(null).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "모든 선제적 제안 삭제 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class)
                    )
            )
    })
    CustomResponse<String> deleteAllSuggestions(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    );
}
