package com.project.backend.domain.event.controller;

import com.project.backend.domain.event.dto.response.EventParticipantResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "일정 공유 API", description = "일정 공유 관련 API")
public interface EventParticipantDocs {

    @Operation(
            summary = "공유 중인 일정 목록 조회",
            description = """
                    로그인한 사용자가 현재 **수락(ACCEPTED)** 한 공유 일정 목록을 조회합니다.
                    
                    ### 조회 대상
                    - 내가 초대를 수락해서 참여 중인 일정
                    - 주최자(owner)의 일정 정보가 함께 내려갑니다.
                    
                    ### 응답 특징
                    - 초대를 수락한 일정만 조회됩니다.
                    - 아직 수락하지 않은 초대(PENDING)는 포함되지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공유 중인 일정 목록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = EventParticipantResDTO.SharedEventsRes.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @GetMapping("/shared-events")
    CustomResponse<EventParticipantResDTO.SharedEventsRes> getSharedEvents(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "일정 참여 초대 목록 조회",
            description = """
                    로그인한 사용자가 받은 **대기 중(PENDING)** 인 일정 공유 초대 목록을 조회합니다.
                    
                    ### 조회 대상
                    - 아직 수락/거절하지 않은 일정 초대
                    
                    ### 응답 특징
                    - 각 초대 항목에는 해당 일정의 **수락 완료 참여자 수(ACCEPTED 기준)** 가 함께 포함됩니다.
                    - 거절되었거나 이미 수락된 초대는 포함되지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "일정 참여 초대 목록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = EventParticipantResDTO.InvitationRes.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @GetMapping("/invitations")
    CustomResponse<EventParticipantResDTO.InvitationRes> getEventInvitations(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "일정 참여 초대 수락",
            description = """
                    일정 공유 초대를 수락합니다.
                    
                    ### 처리 규칙
                    - 전달한 `eventParticipantId` 가 **PENDING 상태** 여야 합니다.
                    - 해당 초대의 대상자가 현재 로그인 사용자 본인이어야 합니다.
                    
                    ### 처리 결과
                    - 초대 상태가 **ACCEPTED** 로 변경됩니다.
                    
                    ### 실패 케이스
                    - 존재하지 않는 초대이거나 이미 처리된 초대인 경우
                    - 다른 사용자의 초대를 수락하려는 경우
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "일정 참여 초대 수락 성공",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "일정 참여 초대 수락 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "200",
                                              "message": "일정 참여 초대 수락 완료",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 초대에 대한 권한이 없는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "EVENT403_1",
                                    summary = "다른 사용자의 일정 공유 초대를 수락하려는 경우",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "EVENT403_1",
                                              "message": "해당 일정 공유 초대에 대한 권한이 없습니다"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 초대가 존재하지 않는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "EVENT404_6",
                                    summary = "존재하지 않거나 이미 처리된 일정 공유 초대",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "EVENT404_6",
                                              "message": "해당 일정 공유 초대가 존재하지 않습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/{eventParticipantId}/acceptance")
    CustomResponse<String> acceptInvitation(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @Parameter(
                    description = "수락할 일정 공유 초대 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long eventParticipantId
    );

    @Operation(
            summary = "일정 참여 초대 거절",
            description = """
                    일정 공유 초대를 거절합니다.
                    
                    ### 처리 규칙
                    - 전달한 `eventParticipantId` 가 **PENDING 상태** 여야 합니다.
                    - 해당 초대의 대상자가 현재 로그인 사용자 본인이어야 합니다.
                    
                    ### 처리 결과
                    - 초대 엔티티가 삭제됩니다.
                    
                    ### 실패 케이스
                    - 존재하지 않는 초대이거나 이미 처리된 초대인 경우
                    - 다른 사용자의 초대를 거절하려는 경우
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "일정 참여 초대 거절 성공",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "일정 참여 초대 거절 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "200",
                                              "message": "일정 참여 초대 거절 완료",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 초대에 대한 권한이 없는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "EVENT403_1",
                                    summary = "다른 사용자의 일정 공유 초대를 거절하려는 경우",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "EVENT403_1",
                                              "message": "해당 일정 공유 초대에 대한 권한이 없습니다"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 초대가 존재하지 않는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "EVENT404_6",
                                    summary = "존재하지 않거나 이미 처리된 일정 공유 초대",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "EVENT404_6",
                                              "message": "해당 일정 공유 초대가 존재하지 않습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/{eventParticipantId}/rejection")
    CustomResponse<String> rejectInvitation(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @Parameter(
                    description = "거절할 일정 공유 초대 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long eventParticipantId
    );
}
