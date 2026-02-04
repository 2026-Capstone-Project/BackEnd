package com.project.backend.domain.nlp.contorller;

import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.dto.response.NlpResDTO;
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

@Tag(name = "자연어 처리 API", description = "자연어를 일정(Event) 또는 할 일(Todo)로 파싱하고 저장하는 API")
public interface NlpDocs {

    // ==================== 파싱 API ====================

    @Operation(
            summary = "자연어 파싱",
            description = """
                ## 개요
                사용자의 자연어 입력을 AI가 분석하여 일정(Event) 또는 할 일(Todo) 정보를 추출합니다.

                ## 사용 흐름
                ```
                1. [프론트] 사용자 입력 텍스트를 /parse API로 전송
                2. [백엔드] AI가 분석하여 파싱 결과 반환
                3. [프론트] 사용자에게 확인 UI 표시 (수정 가능)
                4. [프론트] 확인된 데이터를 /confirm API로 전송하여 저장
                ```

                ## 지원 기능
                | 기능 | 설명 | 예시 |
                |-----|------|-----|
                | 단일 항목 | 하나의 일정/할일 파싱 | "내일 3시 회의" |
                | 복수 항목 | 여러 항목 동시 파싱 | "내일 치과, 금요일까지 보고서" |
                | 반복 일정 | 반복 규칙 감지 | "매주 월수금 헬스" |
                | 상대 날짜 | 자연어 날짜 변환 | "내일", "다음 주 화요일" |
                | 모호함 감지 | 불분명한 입력 처리 | "30일 보고서" → 일정? 할일? |

                ## 반복 주기 (frequency)
                | 값 | 설명 | 필수 필드 |
                |---|------|----------|
                | `DAILY` | 매일 | - |
                | `WEEKLY` | 매주 | daysOfWeek |
                | `MONTHLY` | 매월 | monthlyType + 관련 필드 |
                | `YEARLY` | 매년 | monthOfYear |

                ## 종료 조건 (endType)
                | 값 | 설명 | 필수 필드 |
                |---|------|----------|
                | `NEVER` | 종료 없음 (기본값) | - |
                | `END_BY_DATE` | 특정 날짜까지 | endDate |
                | `END_BY_COUNT` | N회 반복 후 종료 | occurrenceCount |
                """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "자연어 파싱 요청",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = NlpReqDTO.ParseReq.class),
                    examples = {
                            @ExampleObject(
                                    name = "1. 단일 일정",
                                    summary = "내일 오후 3시 팀 미팅",
                                    value = """
                                        {
                                            "text": "내일 오후 3시 팀 미팅",
                                            "baseDate": "2025-01-15"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "2. 할 일 (마감기한)",
                                    summary = "금요일까지 보고서 제출",
                                    value = """
                                        {
                                            "text": "금요일까지 보고서 제출"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "3. 반복 일정 (매주)",
                                    summary = "매주 월수금 저녁 7시 헬스",
                                    value = """
                                        {
                                            "text": "매주 월수금 저녁 7시 헬스"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "4. 반복 일정 (매월 N일)",
                                    summary = "매월 15일 월급날 확인",
                                    value = """
                                        {
                                            "text": "매월 15일 월급날 확인"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "5. 반복 일정 (매월 N번째 요일)",
                                    summary = "매월 셋째 주 화요일 팀 회의",
                                    value = """
                                        {
                                            "text": "매월 셋째 주 화요일 팀 회의"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "6. 반복 일정 (매년)",
                                    summary = "매년 3월 15일 결혼기념일",
                                    value = """
                                        {
                                            "text": "매년 3월 15일 결혼기념일"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "7. 종료 조건 (N회)",
                                    summary = "PT 10회 등록",
                                    value = """
                                        {
                                            "text": "PT 10회 등록"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "8. 복수 항목",
                                    summary = "내일 치과, 금요일까지 보고서",
                                    value = """
                                        {
                                            "text": "내일 치과 예약, 금요일까지 보고서 제출"
                                        }
                                        """
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "파싱 성공",
                    content = @Content(
                            schema = @Schema(implementation = NlpResDTO.ParseRes.class),
                            examples = {
                                    @ExampleObject(
                                            name = "1. 단일 일정 응답",
                                            value = """
                                                {
                                                    "isSuccess": true,
                                                    "code": "COMMON200",
                                                    "message": "파싱 성공",
                                                    "result": {
                                                        "isMultiple": false,
                                                        "totalCount": 1,
                                                        "items": [
                                                            {
                                                                "itemId": "550e8400-e29b-41d4-a716-446655440000",
                                                                "type": "EVENT",
                                                                "title": "팀 미팅",
                                                                "date": "2025-01-16",
                                                                "startTime": "15:00",
                                                                "endTime": "16:00",
                                                                "durationMinutes": 60,
                                                                "isAllDay": false,
                                                                "hasDeadline": false,
                                                                "isRecurring": false,
                                                                "recurrenceRule": null,
                                                                "isAmbiguous": false,
                                                                "ambiguousReason": null,
                                                                "options": null,
                                                                "needsAdditionalInfo": false,
                                                                "additionalInfoType": null,
                                                                "confidence": 0.95
                                                            }
                                                        ]
                                                    }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "2. 반복 일정 응답 (매주)",
                                            value = """
                                                {
                                                    "isSuccess": true,
                                                    "code": "COMMON200",
                                                    "message": "파싱 성공",
                                                    "result": {
                                                        "isMultiple": false,
                                                        "totalCount": 1,
                                                        "items": [
                                                            {
                                                                "itemId": "550e8400-e29b-41d4-a716-446655440001",
                                                                "type": "EVENT",
                                                                "title": "헬스",
                                                                "date": "2025-01-15",
                                                                "startTime": "19:00",
                                                                "endTime": "20:00",
                                                                "durationMinutes": 60,
                                                                "isAllDay": false,
                                                                "hasDeadline": false,
                                                                "isRecurring": true,
                                                                "recurrenceRule": {
                                                                    "frequency": "WEEKLY",
                                                                    "intervalValue": 1,
                                                                    "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
                                                                    "monthlyType": null,
                                                                    "daysOfMonth": null,
                                                                    "weekOfMonth": null,
                                                                    "dayOfWeekInMonth": null,
                                                                    "monthOfYear": null,
                                                                    "endType": "NEVER",
                                                                    "endDate": null,
                                                                    "occurrenceCount": null
                                                                },
                                                                "isAmbiguous": false,
                                                                "ambiguousReason": null,
                                                                "options": null,
                                                                "needsAdditionalInfo": false,
                                                                "additionalInfoType": null,
                                                                "confidence": 0.95
                                                            }
                                                        ]
                                                    }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "3. 반복 일정 응답 (매월 N번째 요일)",
                                            value = """
                                                {
                                                    "isSuccess": true,
                                                    "code": "COMMON200",
                                                    "message": "파싱 성공",
                                                    "result": {
                                                        "isMultiple": false,
                                                        "totalCount": 1,
                                                        "items": [
                                                            {
                                                                "itemId": "550e8400-e29b-41d4-a716-446655440002",
                                                                "type": "EVENT",
                                                                "title": "팀 회의",
                                                                "date": "2025-01-21",
                                                                "startTime": null,
                                                                "endTime": null,
                                                                "durationMinutes": null,
                                                                "isAllDay": false,
                                                                "hasDeadline": false,
                                                                "isRecurring": true,
                                                                "recurrenceRule": {
                                                                    "frequency": "MONTHLY",
                                                                    "intervalValue": 1,
                                                                    "daysOfWeek": null,
                                                                    "monthlyType": "DAY_OF_WEEK",
                                                                    "daysOfMonth": null,
                                                                    "weekOfMonth": 3,
                                                                    "dayOfWeekInMonth": "TUESDAY",
                                                                    "monthOfYear": null,
                                                                    "endType": "NEVER",
                                                                    "endDate": null,
                                                                    "occurrenceCount": null
                                                                },
                                                                "isAmbiguous": false,
                                                                "ambiguousReason": null,
                                                                "options": null,
                                                                "needsAdditionalInfo": false,
                                                                "additionalInfoType": null,
                                                                "confidence": 0.9
                                                            }
                                                        ]
                                                    }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "4. 모호한 입력 응답",
                                            value = """
                                                {
                                                    "isSuccess": true,
                                                    "code": "COMMON200",
                                                    "message": "파싱 성공",
                                                    "result": {
                                                        "isMultiple": false,
                                                        "totalCount": 1,
                                                        "items": [
                                                            {
                                                                "itemId": "550e8400-e29b-41d4-a716-446655440003",
                                                                "type": "AMBIGUOUS",
                                                                "title": "보고서",
                                                                "date": "2025-01-30",
                                                                "startTime": null,
                                                                "endTime": null,
                                                                "durationMinutes": null,
                                                                "isAllDay": true,
                                                                "hasDeadline": false,
                                                                "isRecurring": false,
                                                                "recurrenceRule": null,
                                                                "isAmbiguous": true,
                                                                "ambiguousReason": "일정인지 할 일인지 구분이 어렵습니다",
                                                                "options": [
                                                                    { "label": "보고서 회의 (일정)", "type": "EVENT" },
                                                                    { "label": "보고서 작성 (할 일)", "type": "TODO" }
                                                                ],
                                                                "needsAdditionalInfo": false,
                                                                "additionalInfoType": null,
                                                                "confidence": 0.5
                                                            }
                                                        ]
                                                    }
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (입력 텍스트 누락 등)",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                {
                                    "isSuccess": false,
                                    "code": "NLP001",
                                    "message": "입력 텍스트는 필수입니다.",
                                    "result": null
                                }
                                """
                    ))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스 일시적 오류",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                {
                                    "isSuccess": false,
                                    "code": "NLP004",
                                    "message": "AI 서비스 연결에 실패했습니다. 잠시 후 다시 시도해주세요.",
                                    "result": null
                                }
                                """
                    ))
            )
    })
    CustomResponse<NlpResDTO.ParseRes> parse(
            @Valid @RequestBody NlpReqDTO.ParseReq reqDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    // ==================== 저장 API ====================

    @Operation(
            summary = "파싱 결과 확정 및 저장",
            description = """
                ## 개요
                사용자가 확인/수정한 파싱 결과를 실제 일정(Event) 또는 할 일(Todo)로 저장합니다.

                ## 저장 결과
                | 타입 | 반복 여부 | 생성되는 엔티티 |
                |-----|---------|---------------|
                | EVENT | 단발성 | Event 1개 |
                | EVENT | 반복 | Event 1개 + RecurrenceGroup 1개 |
                | TODO | 단발성 | Todo 1개 |
                | TODO | 반복 | Todo 1개 + TodoRecurrenceGroup 1개 |

                > **참고**: 반복 일정은 조회 시 Generator 패턴으로 동적 생성됩니다.

                ## 필수 필드
                | 필드 | 타입 | 설명 |
                |-----|-----|------|
                | type | `EVENT` / `TODO` | 항목 타입 |
                | title | string | 제목 |
                | date | `YYYY-MM-DD` | 날짜 |
                | isAllDay | boolean | 종일 여부 |
                | isRecurring | boolean | 반복 여부 |

                ## 반복 규칙 (recurrenceRule) 필드

                ### WEEKLY (매주)
                ```json
                {
                    "frequency": "WEEKLY",
                    "intervalValue": 1,
                    "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
                    "endType": "NEVER"
                }
                ```

                ### MONTHLY - DAY_OF_MONTH (매월 N일)
                ```json
                {
                    "frequency": "MONTHLY",
                    "monthlyType": "DAY_OF_MONTH",
                    "daysOfMonth": [15],
                    "endType": "NEVER"
                }
                ```

                ### MONTHLY - DAY_OF_WEEK (매월 N번째 X요일)
                ```json
                {
                    "frequency": "MONTHLY",
                    "monthlyType": "DAY_OF_WEEK",
                    "weekOfMonth": 3,
                    "dayOfWeekInMonth": "TUESDAY",
                    "endType": "NEVER"
                }
                ```

                ### YEARLY (매년)
                ```json
                {
                    "frequency": "YEARLY",
                    "monthOfYear": 3,
                    "daysOfMonth": [15],
                    "endType": "NEVER"
                }
                ```

                ## 요일 값 (daysOfWeek)
                `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

                ## 색상 값 (color)
                `BLUE` (기본값), `GREEN`, `PINK`, `PURPLE`, `GRAY`, `YELLOW`
                """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "파싱 결과 확정 요청",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = NlpReqDTO.ConfirmReq.class),
                    examples = {
                            @ExampleObject(
                                    name = "1. 단일 일정 저장",
                                    summary = "팀 미팅 (일정)",
                                    value = """
                                        {
                                            "items": [
                                                {
                                                    "type": "EVENT",
                                                    "title": "팀 미팅",
                                                    "date": "2025-01-21",
                                                    "startTime": "15:00",
                                                    "endTime": "16:00",
                                                    "isAllDay": false,
                                                    "isRecurring": false,
                                                    "color": "BLUE"
                                                }
                                            ]
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "2. 할 일 저장",
                                    summary = "보고서 제출 (할 일)",
                                    value = """
                                        {
                                            "items": [
                                                {
                                                    "type": "TODO",
                                                    "title": "보고서 제출",
                                                    "date": "2025-01-24",
                                                    "isAllDay": true,
                                                    "isRecurring": false
                                                }
                                            ]
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "3. 반복 일정 (매주 월수금)",
                                    summary = "헬스 - 매주 월수금 저녁 7시",
                                    value = """
                                        {
                                            "items": [
                                                {
                                                    "type": "EVENT",
                                                    "title": "헬스",
                                                    "date": "2025-01-20",
                                                    "startTime": "19:00",
                                                    "endTime": "20:00",
                                                    "isAllDay": false,
                                                    "isRecurring": true,
                                                    "recurrenceRule": {
                                                        "frequency": "WEEKLY",
                                                        "intervalValue": 1,
                                                        "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
                                                        "endType": "NEVER"
                                                    },
                                                    "color": "GREEN"
                                                }
                                            ]
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "4. 반복 일정 (매월 15일)",
                                    summary = "월급날 확인 - 매월 15일",
                                    value = """
                                        {
                                            "items": [
                                                {
                                                    "type": "TODO",
                                                    "title": "월급날 확인",
                                                    "date": "2025-01-15",
                                                    "isAllDay": true,
                                                    "isRecurring": true,
                                                    "recurrenceRule": {
                                                        "frequency": "MONTHLY",
                                                        "intervalValue": 1,
                                                        "monthlyType": "DAY_OF_MONTH",
                                                        "daysOfMonth": [15],
                                                        "endType": "NEVER"
                                                    }
                                                }
                                            ]
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "5. 반복 일정 (매월 셋째 주 화요일)",
                                    summary = "팀 회의 - 매월 셋째 주 화요일",
                                    value = """
                                        {
                                            "items": [
                                                {
                                                    "type": "EVENT",
                                                    "title": "팀 회의",
                                                    "date": "2025-01-21",
                                                    "startTime": "14:00",
                                                    "endTime": "15:00",
                                                    "isAllDay": false,
                                                    "isRecurring": true,
                                                    "recurrenceRule": {
                                                        "frequency": "MONTHLY",
                                                        "intervalValue": 1,
                                                        "monthlyType": "DAY_OF_WEEK",
                                                        "weekOfMonth": 3,
                                                        "dayOfWeekInMonth": "TUESDAY",
                                                        "endType": "NEVER"
                                                    }
                                                }
                                            ]
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "6. 반복 일정 (매년)",
                                    summary = "결혼기념일 - 매년 3월 15일",
                                    value = """
                                        {
                                            "items": [
                                                {
                                                    "type": "EVENT",
                                                    "title": "결혼기념일",
                                                    "date": "2025-03-15",
                                                    "isAllDay": true,
                                                    "isRecurring": true,
                                                    "recurrenceRule": {
                                                        "frequency": "YEARLY",
                                                        "intervalValue": 1,
                                                        "monthOfYear": 3,
                                                        "daysOfMonth": [15],
                                                        "endType": "NEVER"
                                                    },
                                                    "color": "PINK"
                                                }
                                            ]
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "7. 종료 조건 (N회 반복)",
                                    summary = "PT 10회",
                                    value = """
                                        {
                                            "items": [
                                                {
                                                    "type": "EVENT",
                                                    "title": "PT",
                                                    "date": "2025-01-15",
                                                    "startTime": "10:00",
                                                    "endTime": "11:00",
                                                    "isAllDay": false,
                                                    "isRecurring": true,
                                                    "recurrenceRule": {
                                                        "frequency": "WEEKLY",
                                                        "intervalValue": 1,
                                                        "daysOfWeek": ["TUESDAY", "THURSDAY"],
                                                        "endType": "END_BY_COUNT",
                                                        "occurrenceCount": 10
                                                    }
                                                }
                                            ]
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "8. 종료 조건 (특정 날짜까지)",
                                    summary = "스터디 - 6월까지 매주 토요일",
                                    value = """
                                        {
                                            "items": [
                                                {
                                                    "type": "EVENT",
                                                    "title": "스터디",
                                                    "date": "2025-01-18",
                                                    "startTime": "14:00",
                                                    "endTime": "17:00",
                                                    "isAllDay": false,
                                                    "isRecurring": true,
                                                    "recurrenceRule": {
                                                        "frequency": "WEEKLY",
                                                        "intervalValue": 1,
                                                        "daysOfWeek": ["SATURDAY"],
                                                        "endType": "END_BY_DATE",
                                                        "endDate": "2025-06-30"
                                                    },
                                                    "color": "PURPLE"
                                                }
                                            ]
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "9. 복수 항목 저장",
                                    summary = "일정 + 할 일 동시 저장",
                                    value = """
                                        {
                                            "items": [
                                                {
                                                    "type": "EVENT",
                                                    "title": "치과",
                                                    "date": "2025-01-16",
                                                    "startTime": "15:00",
                                                    "endTime": "16:00",
                                                    "isAllDay": false,
                                                    "isRecurring": false
                                                },
                                                {
                                                    "type": "TODO",
                                                    "title": "보고서 제출",
                                                    "date": "2025-01-17",
                                                    "isAllDay": true,
                                                    "isRecurring": false
                                                }
                                            ]
                                        }
                                        """
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "저장 성공",
                    content = @Content(
                            schema = @Schema(implementation = NlpResDTO.ConfirmRes.class),
                            examples = {
                                    @ExampleObject(
                                            name = "1. 단일 항목 저장 성공",
                                            value = """
                                                {
                                                    "isSuccess": true,
                                                    "code": "COMMON200",
                                                    "message": "저장 성공",
                                                    "result": {
                                                        "totalCount": 1,
                                                        "successCount": 1,
                                                        "failCount": 0,
                                                        "results": [
                                                            {
                                                                "savedId": 123,
                                                                "type": "EVENT",
                                                                "title": "헬스",
                                                                "isRecurring": true,
                                                                "success": true,
                                                                "errorMessage": null
                                                            }
                                                        ],
                                                        "message": "일정이 등록되었어요!"
                                                    }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "2. 복수 항목 저장 성공",
                                            value = """
                                                {
                                                    "isSuccess": true,
                                                    "code": "COMMON200",
                                                    "message": "저장 성공",
                                                    "result": {
                                                        "totalCount": 2,
                                                        "successCount": 2,
                                                        "failCount": 0,
                                                        "results": [
                                                            {
                                                                "savedId": 124,
                                                                "type": "EVENT",
                                                                "title": "치과",
                                                                "isRecurring": false,
                                                                "success": true,
                                                                "errorMessage": null
                                                            },
                                                            {
                                                                "savedId": 45,
                                                                "type": "TODO",
                                                                "title": "보고서 제출",
                                                                "isRecurring": false,
                                                                "success": true,
                                                                "errorMessage": null
                                                            }
                                                        ],
                                                        "message": "2개 일정이 등록되었어요!"
                                                    }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "3. 부분 실패",
                                            value = """
                                                {
                                                    "isSuccess": true,
                                                    "code": "COMMON200",
                                                    "message": "저장 성공",
                                                    "result": {
                                                        "totalCount": 2,
                                                        "successCount": 1,
                                                        "failCount": 1,
                                                        "results": [
                                                            {
                                                                "savedId": 125,
                                                                "type": "EVENT",
                                                                "title": "치과",
                                                                "isRecurring": false,
                                                                "success": true,
                                                                "errorMessage": null
                                                            },
                                                            {
                                                                "savedId": null,
                                                                "type": "TODO",
                                                                "title": "보고서",
                                                                "isRecurring": false,
                                                                "success": false,
                                                                "errorMessage": "저장에 실패했습니다"
                                                            }
                                                        ],
                                                        "message": "1개 등록, 1개 실패했어요."
                                                    }
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "필수 필드 누락",
                                    value = """
                                        {
                                            "isSuccess": false,
                                            "code": "COMMON400",
                                            "message": "제목은 필수입니다.",
                                            "result": null
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "잘못된 타입",
                                    value = """
                                        {
                                            "isSuccess": false,
                                            "code": "NLP003",
                                            "message": "잘못된 항목 타입입니다.",
                                            "result": null
                                        }
                                        """
                            )
                    })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                {
                                    "isSuccess": false,
                                    "code": "MEMBER001",
                                    "message": "회원을 찾을 수 없습니다.",
                                    "result": null
                                }
                                """
                    ))
            )
    })
    CustomResponse<NlpResDTO.ConfirmRes> confirm(
            @Valid @RequestBody NlpReqDTO.ConfirmReq reqDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );
}
