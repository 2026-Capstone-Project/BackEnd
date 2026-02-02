package com.project.backend.domain.todo.controller;

import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.enums.RecurrenceUpdateScope;
import com.project.backend.domain.todo.enums.TodoFilter;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "할 일 API", description = "할 일(Todo) 관리 API by 김지명")
public interface TodoDocs {

    // ===== 생성 =====

    @Operation(
            summary = "할 일 생성",
            description = """
                새로운 할 일을 등록합니다.

                ---
                ## 📋 기본 필드

                | 필드 | 타입 | 필수 | 설명 |
                |------|------|------|------|
                | `title` | String | ✅ | 할 일 제목 (최대 100자) |
                | `dueDate` | LocalDate | ✅ | 마감일 (형식: YYYY-MM-DD) |
                | `dueTime` | LocalTime | ❌ | 마감 시간 (형식: HH:mm, 종일이면 생략) |
                | `isAllDay` | Boolean | ✅ | 종일 여부 (true/false) |
                | `priority` | Priority | ✅ | 우선순위 (HIGH, MEDIUM, LOW) |
                | `memo` | String | ❌ | 메모 |
                | `recurrenceGroup` | Object | ❌ | 반복 설정 (없으면 단일 할 일) |

                ---
                ## 🔄 반복 설정 (recurrenceGroup)

                > **null이면 단일 할 일**, 값이 있으면 반복 할 일로 생성됩니다.

                | 필드 | 타입 | 필수 | 설명 |
                |------|------|------|------|
                | `frequency` | RecurrenceFrequency | ✅ | 반복 주기 |
                | `intervalValue` | Integer | ❌ | 반복 간격 (기본값: 1) |
                | `endType` | RecurrenceEndType | ✅ | 종료 조건 |
                | `endDate` | LocalDate | 조건부 | endType이 UNTIL_DATE일 때 필수 |
                | `occurrenceCount` | Integer | 조건부 | endType이 AFTER_COUNT일 때 필수 |

                ### 반복 주기 (frequency)
                - `DAILY`: 매일 반복
                - `WEEKLY`: 매주 반복 → `daysOfWeek` 필요
                - `MONTHLY`: 매월 반복 → `monthlyType` 필요
                - `YEARLY`: 매년 반복

                ### 종료 조건 (endType)
                - `NEVER`: 무한 반복
                - `UNTIL_DATE`: 특정 날짜까지 → `endDate` 필수
                - `AFTER_COUNT`: 특정 횟수만큼 → `occurrenceCount` 필수

                ---
                ## 📅 주간 반복 (WEEKLY)

                | 필드 | 타입 | 필수 | 설명 |
                |------|------|------|------|
                | `daysOfWeek` | List<DayOfWeek> | ✅ | 반복할 요일 목록 |

                **daysOfWeek 값:** `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

                ---
                ## 📆 월간 반복 (MONTHLY)

                | 필드 | 타입 | 필수 | 설명 |
                |------|------|------|------|
                | `monthlyType` | MonthlyType | ✅ | 월간 반복 방식 |

                ### monthlyType = DAY_OF_MONTH (매월 N일)
                | 필드 | 타입 | 필수 | 설명 |
                |------|------|------|------|
                | `daysOfMonth` | List<Integer> | ❌ | 반복할 날짜 (기본값: 시작일) |

                예: 매월 15일, 30일 → `daysOfMonth: [15, 30]`

                ### monthlyType = DAY_OF_WEEK (매월 N번째 X요일)
                | 필드 | 타입 | 필수 | 설명 |
                |------|------|------|------|
                | `weekOfMonth` | Integer | ✅ | 몇 번째 주 (1~5, -1=마지막) |
                | `dayOfWeekInMonth` | DayOfWeek | ✅ | 요일 |

                예: 매월 두 번째 화요일 → `weekOfMonth: 2`, `dayOfWeekInMonth: "TUESDAY"`

                ---
                ## 📅 연간 반복 (YEARLY)

                | 필드 | 타입 | 필수 | 설명 |
                |------|------|------|------|
                | `monthOfYear` | Integer | ❌ | 반복할 월 (1~12, 기본값: 시작월) |
                | `daysOfMonth` | List<Integer> | ❌ | 반복할 날짜 |
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "할 일 등록 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    CustomResponse<TodoResDTO.TodoInfo> createTodo(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = TodoReqDTO.CreateTodo.class),
                            examples = {
                                    @ExampleObject(name = "1. 단일 할 일", value = """
                                        {
                                            "title": "과제 제출",
                                            "dueDate": "2025-12-31",
                                            "dueTime": "23:00",
                                            "isAllDay": false,
                                            "priority": "HIGH",
                                            "memo": "교수님 메일 확인"
                                        }
                                        """),
                                    @ExampleObject(name = "2. 종일 할 일", value = """
                                        {
                                            "title": "휴가",
                                            "dueDate": "2025-01-15",
                                            "isAllDay": true,
                                            "priority": "LOW"
                                        }
                                        """),
                                    @ExampleObject(name = "3. 매일 반복", value = """
                                        {
                                            "title": "약 먹기",
                                            "dueDate": "2025-01-01",
                                            "dueTime": "09:00",
                                            "isAllDay": false,
                                            "priority": "HIGH",
                                            "recurrenceGroup": {
                                                "frequency": "DAILY",
                                                "intervalValue": 1,
                                                "endType": "AFTER_COUNT",
                                                "occurrenceCount": 30
                                            }
                                        }
                                        """),
                                    @ExampleObject(name = "4. 매주 월/수/금 반복", value = """
                                        {
                                            "title": "운동",
                                            "dueDate": "2025-01-06",
                                            "dueTime": "07:00",
                                            "isAllDay": false,
                                            "priority": "MEDIUM",
                                            "recurrenceGroup": {
                                                "frequency": "WEEKLY",
                                                "intervalValue": 1,
                                                "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
                                                "endType": "UNTIL_DATE",
                                                "endDate": "2025-03-31"
                                            }
                                        }
                                        """),
                                    @ExampleObject(name = "5. 격주 토요일 반복", value = """
                                        {
                                            "title": "청소",
                                            "dueDate": "2025-01-04",
                                            "isAllDay": true,
                                            "priority": "MEDIUM",
                                            "recurrenceGroup": {
                                                "frequency": "WEEKLY",
                                                "intervalValue": 2,
                                                "daysOfWeek": ["SATURDAY"],
                                                "endType": "NEVER"
                                            }
                                        }
                                        """),
                                    @ExampleObject(name = "6. 매월 15일 반복", value = """
                                        {
                                            "title": "월세 납부",
                                            "dueDate": "2025-01-15",
                                            "isAllDay": true,
                                            "priority": "HIGH",
                                            "recurrenceGroup": {
                                                "frequency": "MONTHLY",
                                                "intervalValue": 1,
                                                "monthlyType": "DAY_OF_MONTH",
                                                "daysOfMonth": [15],
                                                "endType": "NEVER"
                                            }
                                        }
                                        """),
                                    @ExampleObject(name = "7. 매월 두 번째 화요일 반복", value = """
                                        {
                                            "title": "정기 회의",
                                            "dueDate": "2025-01-14",
                                            "dueTime": "14:00",
                                            "isAllDay": false,
                                            "priority": "HIGH",
                                            "recurrenceGroup": {
                                                "frequency": "MONTHLY",
                                                "intervalValue": 1,
                                                "monthlyType": "DAY_OF_WEEK",
                                                "weekOfMonth": 2,
                                                "dayOfWeekInMonth": "TUESDAY",
                                                "endType": "AFTER_COUNT",
                                                "occurrenceCount": 12
                                            }
                                        }
                                        """),
                                    @ExampleObject(name = "8. 매년 1월 1일 반복", value = """
                                        {
                                            "title": "새해 목표 점검",
                                            "dueDate": "2025-01-01",
                                            "isAllDay": true,
                                            "priority": "MEDIUM",
                                            "recurrenceGroup": {
                                                "frequency": "YEARLY",
                                                "intervalValue": 1,
                                                "monthOfYear": 1,
                                                "daysOfMonth": [1],
                                                "endType": "NEVER"
                                            }
                                        }
                                        """)
                            }
                    )
            )
            @Valid @RequestBody TodoReqDTO.CreateTodo reqDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    // ===== 조회 =====

    @Operation(
            summary = "할 일 목록 조회",
            description = """
                할 일 목록을 조회합니다.

                **필터 옵션:**
                - ALL: 전체
                - TODAY: 오늘
                - PRIORITY: 우선순위순
                - COMPLETED: 완료된 것만

                반복 할 일은 다음 1개만 표시됩니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    CustomResponse<TodoResDTO.TodoListRes> getTodos(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "필터", example = "ALL")
            @RequestParam(defaultValue = "ALL") TodoFilter filter
    );

    @Operation(
            summary = "캘린더용 할 일 조회",
            description = """
                특정 기간의 할 일을 조회합니다.
                반복 할 일은 해당 기간 내 모든 날짜로 펼쳐집니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    CustomResponse<TodoResDTO.TodoListRes> getTodosForCalendar(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "시작 날짜", example = "2025-01-01") @RequestParam LocalDate startDate,
            @Parameter(description = "종료 날짜", example = "2025-01-31") @RequestParam LocalDate endDate
    );

    @Operation(
            summary = "할 일 상세 조회",
            description = """
                할 일 상세 정보를 조회합니다.
                반복 할 일인 경우 occurrenceDate가 필수입니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "할 일을 찾을 수 없음")
    })
    CustomResponse<TodoResDTO.TodoDetailRes> getTodoDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "할 일 ID", example = "1") @PathVariable Long todoId,
            @Parameter(description = "반복 할 일의 특정 날짜", example = "2025-01-15")
            @RequestParam(required = false) LocalDate occurrenceDate
    );

    @Operation(
            summary = "진행 상황 조회",
            description = "특정 날짜의 할 일 진행 상황을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    CustomResponse<TodoResDTO.TodoProgressRes> getProgress(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "조회 날짜", example = "2025-01-15") @RequestParam LocalDate date
    );

    // ===== 수정 =====

    @Operation(
            summary = "할 일 수정",
            description = """
                할 일을 수정합니다.

                ---
                ## 📝 파라미터 설명

                | 파라미터 | 타입 | 단일 할 일 | 반복 할 일 | 설명 |
                |----------|------|-----------|-----------|------|
                | `todoId` | Long | ✅ 필수 | ✅ 필수 | 수정할 할 일 ID |
                | `occurrenceDate` | LocalDate | ❌ 불필요 | ✅ 필수 | 수정 기준 날짜 |
                | `scope` | RecurrenceUpdateScope | ❌ 불필요 | ✅ 필수 | 수정 범위 |

                ---
                ## 🔄 반복 할 일 수정 범위 (scope)

                | 값 | 설명 | 동작 |
                |----|------|------|
                | `THIS_TODO` | 이 할 일만 | 해당 날짜에 예외(OVERRIDE) 생성 |
                | `THIS_AND_FOLLOWING` | 이 할 일 및 이후 | 기존 반복 종료 + 새 반복 생성 |
                | `ALL_TODOS` | 모든 할 일 | 원본 할 일 직접 수정 |

                ---
                ## 📋 수정 가능한 필드 (모두 선택)

                | 필드 | 타입 | 설명 |
                |------|------|------|
                | `title` | String | 제목 (최대 100자) |
                | `dueDate` | LocalDate | 마감일 |
                | `dueTime` | LocalTime | 마감 시간 |
                | `isAllDay` | Boolean | 종일 여부 |
                | `priority` | Priority | 우선순위 (HIGH, MEDIUM, LOW) |
                | `memo` | String | 메모 |
                | `recurrenceGroup` | Object | 반복 설정 (THIS_AND_FOLLOWING만 적용) |

                > ⚠️ **주의:** 수정하지 않을 필드는 null로 보내거나 생략하세요.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "할 일을 찾을 수 없음")
    })
    CustomResponse<TodoResDTO.TodoInfo> updateTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "할 일 ID", example = "1") @PathVariable Long todoId,
            @Parameter(description = "반복 할 일의 수정 기준 날짜 (반복 할 일인 경우 필수)", example = "2025-01-15")
            @RequestParam(required = false) LocalDate occurrenceDate,
            @Parameter(description = "반복 할 일 수정 범위 (반복 할 일인 경우 필수)", schema = @Schema(allowableValues = {"THIS_TODO", "THIS_AND_FOLLOWING", "ALL_TODOS"}))
            @RequestParam(required = false) RecurrenceUpdateScope scope,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = TodoReqDTO.UpdateTodo.class),
                            examples = {
                                    @ExampleObject(name = "제목만 수정", value = """
                                        {
                                            "title": "수정된 제목"
                                        }
                                        """),
                                    @ExampleObject(name = "우선순위와 메모 수정", value = """
                                        {
                                            "priority": "HIGH",
                                            "memo": "급한 일!"
                                        }
                                        """),
                                    @ExampleObject(name = "마감일 변경", value = """
                                        {
                                            "dueDate": "2025-02-01",
                                            "dueTime": "18:00"
                                        }
                                        """)
                            }
                    )
            )
            @Valid @RequestBody TodoReqDTO.UpdateTodo reqDTO
    );

    @Operation(
            summary = "할 일 완료 상태 변경",
            description = """
                할 일의 완료/미완료 상태를 변경합니다.

                ---
                ## 📝 파라미터 설명

                | 파라미터 | 타입 | 단일 할 일 | 반복 할 일 | 설명 |
                |----------|------|-----------|-----------|------|
                | `todoId` | Long | ✅ 필수 | ✅ 필수 | 할 일 ID |
                | `occurrenceDate` | LocalDate | ❌ 불필요 | ✅ 필수 | 완료 처리할 날짜 |
                | `isCompleted` | boolean | ✅ 필수 | ✅ 필수 | 완료 여부 |

                ---
                ## 🔄 동작 방식

                **단일 할 일:**
                - 할 일의 완료 상태를 직접 변경

                **반복 할 일:**
                - 해당 날짜에 완료/미완료 예외를 생성
                - 다른 날짜의 완료 상태에는 영향 없음

                ---
                ## 💡 예시

                ```
                PATCH /api/v1/todos/1/complete?isCompleted=true
                → 단일 할 일 완료 처리

                PATCH /api/v1/todos/1/complete?occurrenceDate=2025-01-15&isCompleted=true
                → 반복 할 일의 2025-01-15만 완료 처리
                ```
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "할 일을 찾을 수 없음")
    })
    CustomResponse<TodoResDTO.TodoCompleteRes> updateCompleteStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "할 일 ID", example = "1") @PathVariable Long todoId,
            @Parameter(description = "반복 할 일의 완료 처리할 날짜 (반복 할 일인 경우 필수)", example = "2025-01-15")
            @RequestParam(required = false) LocalDate occurrenceDate,
            @Parameter(description = "완료 여부 (true: 완료, false: 미완료)", example = "true") @RequestParam boolean isCompleted
    );

    // ===== 삭제 =====

    @Operation(
            summary = "할 일 삭제",
            description = """
                할 일을 삭제합니다.

                ---
                ## 📝 파라미터 설명

                | 파라미터 | 타입 | 단일 할 일 | 반복 할 일 | 설명 |
                |----------|------|-----------|-----------|------|
                | `todoId` | Long | ✅ 필수 | ✅ 필수 | 삭제할 할 일 ID |
                | `occurrenceDate` | LocalDate | ❌ 불필요 | ✅ 필수 | 삭제 기준 날짜 |
                | `scope` | RecurrenceUpdateScope | ❌ 불필요 | ✅ 필수 | 삭제 범위 |

                ---
                ## 🔄 반복 할 일 삭제 범위 (scope)

                | 값 | 설명 | 동작 |
                |----|------|------|
                | `THIS_TODO` | 이 할 일만 | 해당 날짜에 SKIP 예외 생성 (건너뛰기) |
                | `THIS_AND_FOLLOWING` | 이 할 일 및 이후 | 반복 종료일을 전날로 변경 |
                | `ALL_TODOS` | 모든 할 일 | 할 일과 반복 그룹 모두 삭제 |

                ---
                ## 💡 예시

                ```
                DELETE /api/v1/todos/1
                → 단일 할 일 삭제

                DELETE /api/v1/todos/1?occurrenceDate=2025-01-15&scope=THIS_TODO
                → 반복 할 일의 2025-01-15만 건너뛰기

                DELETE /api/v1/todos/1?occurrenceDate=2025-01-15&scope=THIS_AND_FOLLOWING
                → 2025-01-15부터 모든 반복 삭제 (2025-01-14까지만 유지)

                DELETE /api/v1/todos/1?occurrenceDate=2025-01-15&scope=ALL_TODOS
                → 반복 할 일 전체 삭제
                ```
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (반복 할 일에 필수 파라미터 누락)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "할 일을 찾을 수 없음")
    })
    CustomResponse<Void> deleteTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "할 일 ID", example = "1") @PathVariable Long todoId,
            @Parameter(description = "반복 할 일의 삭제 기준 날짜 (반복 할 일인 경우 필수)", example = "2025-01-15")
            @RequestParam(required = false) LocalDate occurrenceDate,
            @Parameter(description = "반복 할 일 삭제 범위 (반복 할 일인 경우 필수)", schema = @Schema(allowableValues = {"THIS_TODO", "THIS_AND_FOLLOWING", "ALL_TODOS"}))
            @RequestParam(required = false) RecurrenceUpdateScope scope
    );
}
