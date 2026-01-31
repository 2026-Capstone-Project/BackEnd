package com.project.backend.domain.todo.controller;

import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
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
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "할 일 API", description = "할 일(Todo) 관리 API")
public interface TodoDocs {

    @Operation(
            summary = "할 일 등록",
            description = """
                새로운 할 일을 등록합니다.

                **반복 할 일 설정:**
                - recurrence가 null이면 일반 할 일로 등록됩니다.
                - recurrence가 있으면 반복 할 일로 등록되며, RecurringTodo가 함께 생성됩니다.

                **반복 유형:**
                - DAILY: 매일 반복
                - WEEKLY: 매주 반복 (customDays로 요일 지정 가능)
                - MONTHLY: 매월 반복 (customDays로 날짜 지정 가능)
                - YEARLY: 매년 반복

                **종료 조건:**
                - endDate: 특정 날짜까지 반복
                - repeatCount: 특정 횟수만큼 반복
                - 둘 다 null이면 무한 반복
                """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "할 일 등록 요청",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = TodoReqDTO.CreateTodo.class),
                    examples = {
                            @ExampleObject(
                                    name = "일반 할 일",
                                    value = """
                                        {
                                            "title": "과제 제출",
                                            "dueDate": "2025-12-31",
                                            "dueTime": "23:00",
                                            "isAllDay": false,
                                            "priority": "HIGH",
                                            "memo": "교수님 메일 확인",
                                            "recurrence": null
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "반복 할 일 - 매일",
                                    value = """
                                        {
                                            "title": "영어 단어 암기",
                                            "dueDate": "2025-12-31",
                                            "dueTime": null,
                                            "isAllDay": true,
                                            "priority": "LOW",
                                            "memo": null,
                                            "recurrence": {
                                                "type": "DAILY",
                                                "customDays": null,
                                                "endDate": "2026-01-07",
                                                "repeatCount": null
                                            }
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "반복 할 일 - 매주 목, 금",
                                    value = """
                                        {
                                            "title": "알바",
                                            "dueDate": "2025-12-31",
                                            "dueTime": "17:00",
                                            "isAllDay": false,
                                            "priority": "MEDIUM",
                                            "memo": null,
                                            "recurrence": {
                                                "type": "WEEKLY",
                                                "customDays": ["THU", "FRI"],
                                                "endDate": "2026-01-07",
                                                "repeatCount": null
                                            }
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "반복 할 일 - 매월 16일, 31일",
                                    value = """
                                        {
                                            "title": "시험",
                                            "dueDate": "2025-12-31",
                                            "dueTime": "17:00",
                                            "isAllDay": false,
                                            "priority": "HIGH",
                                            "memo": null,
                                            "recurrence": {
                                                "type": "MONTHLY",
                                                "customDays": ["16", "31"],
                                                "endDate": "2026-06-30",
                                                "repeatCount": null
                                            }
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "반복 할 일 - 4번 반복",
                                    value = """
                                        {
                                            "title": "정기 회의",
                                            "dueDate": "2025-12-31",
                                            "dueTime": "10:00",
                                            "isAllDay": false,
                                            "priority": "MEDIUM",
                                            "memo": null,
                                            "recurrence": {
                                                "type": "WEEKLY",
                                                "customDays": null,
                                                "endDate": null,
                                                "repeatCount": 4
                                            }
                                        }
                                        """
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "할일 등록 완료",
                    content = @Content(schema = @Schema(implementation = TodoResDTO.TodoInfo.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락, 유효성 검증 실패)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음"
            )
    })
    CustomResponse<TodoResDTO.TodoInfo> createTodo(
            @Valid @RequestBody TodoReqDTO.CreateTodo reqDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );
}
