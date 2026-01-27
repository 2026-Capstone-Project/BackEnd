package com.project.backend.domain.event.controller;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "일정(Event) API", description = "일정 생성 API")
public interface EventDocs {

    @Operation(
            summary = "일정 생성",
            description = """
                새로운 일정을 생성합니다.

                ## 일정(Event) 필수 파라미터

                - title (String)
                  - 일정 제목
                - startTime (LocalDateTime)
                  - 일정 시작 일시
                  - ISO-8601 형식 (예: 2026-01-27T10:00:00)
                - endTime (LocalDateTime)
                  - 일정 종료 일시

                ## 일정(Event) 선택 파라미터

                - content (String)
                  - 일정 메모
                - location (String)
                  - 장소 정보 (추후 지도 서비스 연동 예정)
                - isAllDay (Boolean)
                  - 종일 일정 여부
                  - 미전송 시 false 처리
                - color (EventColor)
                  - 일정 색상
                  - 미전송 시 기본값 BLUE 적용
                  - 사용 가능 값:
                    - BLUE
                    - GREEN
                    - PINK
                    - PURPLE
                    - GRAY
                    - YELLOW

                ## 반복 일정 처리 규칙

                - 반복을 사용하지 않는 경우
                  → recurrenceGroup 필드는 **아예 보내지 않습니다**
                - 반복을 사용하는 경우에만
                  → recurrenceGroup 객체를 포함합니다

                ## 🔁 반복 일정 파라미터 (recurrenceGroup)

                ### 공통 필수 필드
                - frequency (RecurrenceFrequency)
                  - DAILY / WEEKLY / MONTHLY / YEARLY
                  
                - endType (RecurrenceEndType)
                  - NEVER
                  - END_BY_DATE
                  - END_BY_COUNT

                ---
                ### WEEKLY (매주 반복)
                - daysOfWeek (List<String>)
                  - 예: ["MON", "WED", "FRI"]

                ---
                ### MONTHLY (매월 반복)

                - monthlyType (MonthlyType)
                  - DAY_OF_MONTH : 매월 N일
                  - DAY_OF_WEEK : 매월 N번째 X요일

                #### monthlyType = DAY_OF_MONTH
                - daysOfMonth (List<Integer>)
                  - 예: [15]

                #### monthlyType = DAY_OF_WEEK
                - weekOfMonth (Integer)
                  - 예: 2 (두 번째)
                - dayOfWeekInMonth (String)
                  - 예: "MON", "TUE"

                ---
                ### YEARLY (매년 반복)
                - monthOfYear (Integer)
                  - 1 ~ 12
                - daysOfMonth (Integer)
                  - 1 ~ 31

                ---
                ## 🔚 반복 종료 조건

                - endType = NEVER
                  - 종료 없음
                - endType = END_BY_DATE
                  - endDate 필수
                - endType = END_BY_COUNT
                  - occurrenceCount 필수
                """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "일정 생성 요청",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EventReqDTO.CreateReq.class),
                    examples = {

                            // ---------------- 단일 일정 ----------------
                            @ExampleObject(
                                    name = "단일 일정",
                                    description = "반복 없는 단일 일정 (recurrenceGroup 미포함)",
                                    value = """
                                        {
                                          "title": "팀 미팅",
                                          "content": "주간 회의",
                                          "startTime": "2026-01-27T10:00:00",
                                          "endTime": "2026-01-27T11:00:00",
                                          "location": "회의실 A",
                                          "color": "BLUE",
                                          "isAllDay": false
                                        }
                                        """
                            ),
                            // ---------------- 일간 반복 ----------------
                            @ExampleObject(
                                    name = "매일 반복 일정 (최소 입력)",
                                    description = "frequency만 DAILY로 설정한 기본 매일 반복",
                                    value = """
                                        {
                                          "title": "일일 스탠드업",
                                          "startTime": "2026-01-27T09:00:00",
                                          "endTime": "2026-01-27T09:15:00",
                                          "recurrenceGroup": {
                                            "frequency": "DAILY",
                                            "endType": "NEVER"
                                          }
                                        }
                                        """
                            ),

                            // ---------------- 주간 반복 ----------------
                            @ExampleObject(
                                    name = "주간 반복 일정",
                                    description = "매주 월/수/금 반복",
                                    value = """
                                        {
                                          "title": "헬스장",
                                          "startTime": "2026-01-27T19:00:00",
                                          "endTime": "2026-01-27T20:30:00",
                                          "isAllDay": false,
                                          "recurrenceGroup": {
                                            "frequency": "WEEKLY",
                                            "daysOfWeek": ["MON", "WED", "FRI"],
                                            "endType": "END_BY_DATE",
                                            "endDate": "2026-04-30"
                                          }
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "주간 반복 일정 (최소 입력)",
                                    description = "frequency만 WEEKLY로 설정, 요일은 startTime 기준 자동 설정",
                                    value = """
                                        {
                                          "title": "주간 회의",
                                          "startTime": "2026-01-27T10:00:00",
                                          "endTime": "2026-01-27T11:00:00",
                                          "recurrenceGroup": {
                                            "frequency": "WEEKLY",
                                            "endType": "NEVER"
                                          }
                                        }
                                        """
                            ),

                            // ---------------- 월간 반복 ----------------
                            @ExampleObject(
                                    name = "월간 반복 일정 (매월 N일)",
                                    description = "매월 15일 반복",
                                    value = """
                                        {
                                          "title": "월급날 확인",
                                          "startTime": "2026-01-15T09:00:00",
                                          "endTime": "2026-01-15T09:30:00",
                                          "location": "회의실 A",
                                          "color": "BLUE",
                                          "recurrenceGroup": {
                                            "frequency": "MONTHLY",
                                            "monthlyType": "DAY_OF_MONTH",
                                            "daysOfMonth": [15],
                                            "endType": "END_BY_COUNT",
                                            "occurrenceCount": 6
                                          }
                                        }
                                        """
                            ),

                            @ExampleObject(
                                    name = "월간 반복 일정 (매월 N번째 X요일)",
                                    description = "매월 2번째 월요일",
                                    value = """
                                        {
                                          "title": "월급날 확인",
                                          "startTime": "2026-01-15T09:00:00",
                                          "endTime": "2026-01-15T09:30:00",
                                          "color": "GREEN",
                                          "recurrenceGroup": {
                                            "frequency": "MONTHLY",
                                            "monthlyType": "DAY_OF_WEEK",
                                            "weekOfMonth": 2,
                                            "dayOfWeekInMonth": ["TUE"],
                                            "endType": "NEVER"
                                          }
                                        }
                                        """
                            ),

                            @ExampleObject(
                                    name = "월간 반복 일정 (매월 N일, 최소 입력)",
                                    description = "monthlyType DAY_OF_MONTH, 날짜는 startTime 기준 자동 설정",
                                    value = """
                                        {
                                          "title": "월간 정산",
                                          "startTime": "2026-01-15T09:00:00",
                                          "endTime": "2026-01-15T09:30:00",
                                          "recurrenceGroup": {
                                            "frequency": "MONTHLY",
                                            "monthlyType": "DAY_OF_MONTH",
                                            "endType": "NEVER"
                                          }
                                        }
                                        """
                            ),

                            @ExampleObject(
                                    name = "월간 반복 일정 (매월 N번째 요일, 최소 입력)",
                                    description = "monthlyType DAY_OF_WEEK, 주차/요일은 startTime 기준 자동 설정",
                                    value = """
                                        {
                                          "title": "월간 회의",
                                          "startTime": "2026-01-27T14:00:00",
                                          "endTime": "2026-01-27T15:00:00",
                                          "recurrenceGroup": {
                                            "frequency": "MONTHLY",
                                            "monthlyType": "DAY_OF_WEEK",
                                            "endType": "NEVER"
                                          }
                                        }
                                        """
                            ),

                            // ---------------- 연간 반복 ----------------
                            @ExampleObject(
                                    name = "연간 반복 일정",
                                    description = "매년 1월 반복",
                                    value = """
                                        {
                                          "title": "생일",
                                          "startTime": "2026-01-15T09:00:00",
                                          "endTime": "2026-01-15T09:30:00",
                                          "location": "회의실 A",
                                          "recurrenceGroup": {
                                            "frequency": "YEARLY",
                                            "monthOfYear": 6,
                                            "daysOfMonth": [10,15],
                                            "endType": "END_BY_COUNT",
                                            "occurrenceCount": 9
                                          }
                                        }
                                        """
                            ),

                            @ExampleObject(
                                    name = "연간 반복 일정 (최소 입력)",
                                    description = "frequency만 YEARLY로 설정, 월/일은 startTime 기준 자동 설정",
                                    value = """
                                        {
                                          "title": "기념일",
                                          "startTime": "2026-01-15T09:00:00",
                                          "endTime": "2026-01-15T09:30:00",
                                          "recurrenceGroup": {
                                            "frequency": "YEARLY",
                                            "endType": "NEVER"
                                          }
                                        }
                                        """
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "일정 생성 성공",
                    content = @Content(
                            schema = @Schema(implementation = EventResDTO.CreateRes.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락, 반복 규칙 오류 등)"
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
    CustomResponse<EventResDTO.CreateRes> createEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody EventReqDTO.CreateReq createReq
    );
}
