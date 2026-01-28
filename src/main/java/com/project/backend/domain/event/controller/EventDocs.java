package com.project.backend.domain.event.controller;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.dto.response.swagger.EventDetailRes;
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
                    description = "시간필드 값을 설정하지 않았습니다",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "INVALID_TIME",
                                    value = """
                        {
                          "isSuccess": false,
                          "code": "EVENT400_1",
                          "message": "시간을 설정하지 않았습니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "end가 start 보다 더 이전 시간일 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "INVALID_TIME_RANGE",
                                    value = """
                        {
                          "isSuccess": false,
                          "code": "EVENT400_2",
                          "message": "시간 설정이 잘못되었습니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "설정한 반복 타입과 관련 없는 필드에 값이 들어간경우\n" +
                            "EX) WEEKLY 설정인데 monthOfYear 필드에 값이 있는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "INVALID_FREQUENCY_CONDITION",
                                    value = """
                        {
                          "isSuccess": false,
                          "code": "RG400_15",
                          "message": "FREQUENCY 타입에 따른 불필요한 필드값이 채워져 있습니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "설정한 종료 타입과 관련 없는 필드에 값이 들어간경우\n" +
                            "EX) END_BY_COUNT 설정인데 endDate 필드에 값이 있는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "INVALID_END_CONDITION",
                                    value = """
                        {
                          "isSuccess": false,
                          "code": "RG400_1",
                          "message": "EndType 타입에 따른 불필요한 필드값이 채워져 있습니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "EndType이 END_BY_DATE인데, endDate 필드에 값이 없는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "END_DATE_REQUIRED",
                                    value = """
                        {
                          "isSuccess": false,
                          "code": "RG400_2",
                          "message": "종료 날짜가 설정되지 않았습니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "EndType이 END_BY_COUNT인데, occurrenceCount 필드에 값이 없는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "END_COUNT_REQUIRED",
                                    value = """
                        {
                          "isSuccess": false,
                          "code": "RG400_3",
                          "message": "종료 카운트가 설정되지 않았습니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "NEVER, END_BY_DATE, END_BY_COUNT 이외의 값이 EndType 값으로 들어간 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "INVALID_END_TYPE",
                                    value = """
                        {
                          "isSuccess": false,
                          "code": "RG400_4",
                          "message": "잘못된 종료타입입니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "DAY_OF_MONTH, DAY_OF_WEEK 이외의 값이 MonthlyType 값으로 들어간 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "INVALID_MONTHLY_TYPE",
                                    value = """
                        {
                          "isSuccess": false,
                          "code": "RG400_11",
                          "message": "잘못된 월간 타입입니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "endDate 의 값이 생성하려는 일정의 start 값보다 이전인 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "INVALID_END_DATE_RANGE",
                                    value = """
                        {
                          "isSuccess": false,
                          "code": "RG400_13",
                          "message": "종료 날짜가 일정 시작 날짜보다 빠릅니다"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "MON, TUE, WEN, THU, FRI, SAT, SUN 이외의 값이 dayOfWeek 값으로 들어간 경",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "INVALID_DAY_OF_WEEK",
                                    value = """
                        {
                          "isSuc우ess": false,
                          "code": "RG400_14",
                          "message": "잘못된 요일입니다"
                        }
                        """
                            )
                    )
            )

    })
    CustomResponse<EventResDTO.CreateRes> createEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody EventReqDTO.CreateReq createReq
    );

    @Operation(
            summary = "일정 상세 조회",
            description = """
                캘린더에서 선택한 단일 일정의 상세 정보를 조회합니다.

                - 단일 일정인 경우
                  → recurrenceGroup 필드는 null로 반환됩니다.

                - 반복 일정인 경우
                  → 반복 규칙 원본 정보(recurrenceGroup)를 함께 반환합니다.

                해당 API는 일정 수정/삭제 화면에서 사용됩니다.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "일정 상세 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = EventDetailRes.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "일정을 찾을 수 없음",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "EVENT404_3",
                                      "message": "일정을 찾을 수 없습니다"
                                    }
                                    """
                            )
                    )
            )
    })
    CustomResponse<EventResDTO.DetailRes> getEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(
                    description = "조회할 일정 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long eventId
    );
}
