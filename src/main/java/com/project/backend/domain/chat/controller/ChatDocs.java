package com.project.backend.domain.chat.controller;

import com.project.backend.domain.chat.dto.request.ChatReqDTO;
import com.project.backend.domain.chat.dto.response.ChatResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "채팅 API", description = "AI 어시스턴트 Calio와의 대화 API")
public interface ChatDocs {

    @Operation(
            summary = "채팅 메시지 전송",
            description = """
                    Calio AI 어시스턴트에게 메시지를 전송하고 응답을 받습니다.

                    **응답 action 종류**
                    - `CREATED` : 일정 또는 할 일 생성 완료
                    - `UPDATED` : 일정 또는 할 일 수정 완료
                    - `DELETED` : 일정 또는 할 일 삭제 완료
                    - `CLARIFYING` : AI가 추가 정보를 요청하는 중 (반복 일정 범위 확인 등)
                    - `NONE` : 일반 답변, DB 변화 없음

                    **scheduleId / recurrenceGroupId**
                    - `action`이 `NONE` 또는 `CLARIFYING`이면 두 필드 모두 `null`
                    - 반복 일정이 아니면 `recurrenceGroupId`는 `null`

                    **scheduleType**
                    - `EVENT` : 캘린더 일정
                    - `TODO` : 할 일
                    - `action`이 `NONE` 또는 `CLARIFYING`이면 `null`
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ChatReqDTO.SendReq.class),
                    examples = {
                            @ExampleObject(
                                    name = "일정 생성 요청",
                                    value = """
                                            {
                                              "message": "다음 주 월요일 오전 10시에 팀 미팅 잡아줘"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "일반 질문",
                                    value = """
                                            {
                                              "message": "이번 주 일정 알려줘"
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "AI 응답 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "일정 생성 완료",
                                            summary = "action = CREATED",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "code": "COMMON200",
                                                      "message": "챗봇 응답 성공",
                                                      "result": {
                                                        "reply": "다음 주 월요일 오전 10시에 팀 미팅을 등록했어요.",
                                                        "action": "CREATED",
                                                        "scheduleId": 42,
                                                        "recurrenceGroupId": null,
                                                        "scheduleType": "EVENT"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "반복 일정 범위 되묻기",
                                            summary = "action = CLARIFYING",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "code": "COMMON200",
                                                      "message": "챗봇 응답 성공",
                                                      "result": {
                                                        "reply": "이번 회의만 삭제할까요, 아니면 이후 모든 회의를 삭제할까요?",
                                                        "action": "CLARIFYING",
                                                        "scheduleId": null,
                                                        "recurrenceGroupId": null,
                                                        "scheduleType": null
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "일반 답변",
                                            summary = "action = NONE",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "code": "COMMON200",
                                                      "message": "챗봇 응답 성공",
                                                      "result": {
                                                        "reply": "이번 주 일정은 월요일 팀 미팅, 수요일 점심 약속이 있어요.",
                                                        "action": "NONE",
                                                        "scheduleId": null,
                                                        "recurrenceGroupId": null,
                                                        "scheduleType": null
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "메시지가 비어 있음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "EMPTY_MESSAGE",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "CHAT_400",
                                              "message": "메시지가 비어 있습니다.",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "AI 응답 생성 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "CHAT_API_ERROR",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "CHAT_502",
                                              "message": "AI 응답 생성에 실패했습니다.",
                                              "result": null
                                            }
                                            """
                            )
                    )
            )
    })
    CustomResponse<ChatResDTO.SendRes> sendMessage(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @org.springframework.web.bind.annotation.RequestBody
            @Valid ChatReqDTO.SendReq reqDTO
    );

    @Operation(
            summary = "채팅 대화 기록 조회",
            description = """
                    세션 복귀 시 Redis에 저장된 대화 기록을 반환합니다.

                    **messages**
                    - `role` : `user` 또는 `assistant`
                    - `content` : 메시지 내용
                    - 대화 없으면 빈 배열 반환

                    **summary**
                    - 대화 메시지가 임계값(20개)을 초과한 적이 있으면 LLM이 생성한 rolling summary
                    - 요약이 없으면 `null`
                    - TTL: 24시간 (마지막 메시지 저장 기준)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "대화 기록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "대화 기록 있음",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "code": "COMMON200",
                                                      "message": "대화 기록 조회 성공",
                                                      "result": {
                                                        "messages": [
                                                          { "role": "user", "content": "다음 주 월요일 팀 미팅 잡아줘" },
                                                          { "role": "assistant", "content": "다음 주 월요일 오전 10시에 팀 미팅을 등록했어요." }
                                                        ],
                                                        "summary": "사용자가 팀 미팅 일정을 생성 요청했고 완료함."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "대화 기록 없음",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "code": "COMMON200",
                                                      "message": "대화 기록 조회 성공",
                                                      "result": {
                                                        "messages": [],
                                                        "summary": null
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    CustomResponse<ChatResDTO.HistoryRes> getHistory(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails
    );
}
