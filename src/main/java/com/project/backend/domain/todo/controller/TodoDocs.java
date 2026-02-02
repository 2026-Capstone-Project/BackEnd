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

                **반복 설정 (recurrenceGroup):**
                - null이면 단일 할 일로 생성
                - 값이 있으면 반복 할 일로 생성

                **반복 주기 (frequency):**
                - DAILY: 매일
                - WEEKLY: 매주 (daysOfWeek로 요일 지정)
                - MONTHLY: 매월 (monthlyType으로 방식 지정)
                - YEARLY: 매년

                **종료 조건 (endType):**
                - NEVER: 무한 반복
                - UNTIL_DATE: 특정 날짜까지 (endDate 필수)
                - AFTER_COUNT: 특정 횟수 (occurrenceCount 필수)
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
                                    @ExampleObject(name = "단일 할 일", value = """
                                        {
                                            "title": "과제 제출",
                                            "dueDate": "2025-12-31",
                                            "dueTime": "23:00",
                                            "isAllDay": false,
                                            "priority": "HIGH",
                                            "memo": "교수님 메일 확인"
                                        }
                                        """),
                                    @ExampleObject(name = "반복 할 일 - 매주 월/수/금", value = """
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

                **반복 할 일 수정 시:**
                - occurrenceDate: 수정 기준 날짜 (필수)
                - scope: 수정 범위 (필수)
                  - THIS_TODO: 이 할 일만
                  - THIS_AND_FOLLOWING: 이 할 일 및 이후 모든 할 일
                  - ALL_TODOS: 모든 할 일
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
            @Parameter(description = "반복 할 일의 수정 기준 날짜", example = "2025-01-15")
            @RequestParam(required = false) LocalDate occurrenceDate,
            @Parameter(description = "반복 할 일 수정 범위", example = "THIS_TODO")
            @RequestParam(required = false) RecurrenceUpdateScope scope,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = TodoReqDTO.UpdateTodo.class))
            )
            @Valid @RequestBody TodoReqDTO.UpdateTodo reqDTO
    );

    @Operation(
            summary = "할 일 완료 상태 변경",
            description = """
                할 일의 완료 상태를 변경합니다.
                반복 할 일인 경우 occurrenceDate가 필수입니다.
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
            @Parameter(description = "반복 할 일의 특정 날짜", example = "2025-01-15")
            @RequestParam(required = false) LocalDate occurrenceDate,
            @Parameter(description = "완료 여부", example = "true") @RequestParam boolean isCompleted
    );

    // ===== 삭제 =====

    @Operation(
            summary = "할 일 삭제",
            description = """
                할 일을 삭제합니다.

                **반복 할 일 삭제 시:**
                - occurrenceDate: 삭제 기준 날짜 (필수)
                - scope: 삭제 범위 (필수)
                  - THIS_TODO: 이 할 일만 (해당 날짜 건너뛰기)
                  - THIS_AND_FOLLOWING: 이 할 일 및 이후 모든 할 일
                  - ALL_TODOS: 모든 할 일 (전체 삭제)
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "할 일을 찾을 수 없음")
    })
    CustomResponse<Void> deleteTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "할 일 ID", example = "1") @PathVariable Long todoId,
            @Parameter(description = "반복 할 일의 삭제 기준 날짜", example = "2025-01-15")
            @RequestParam(required = false) LocalDate occurrenceDate,
            @Parameter(description = "반복 할 일 삭제 범위", example = "THIS_TODO")
            @RequestParam(required = false) RecurrenceUpdateScope scope
    );
}
