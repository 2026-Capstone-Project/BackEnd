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
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

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
                캘린더에서 선택한 일정(단일 또는 반복)의 상세 정보를 조회합니다.
               
                ### 요청 파라미터
                - eventId (PathVariable)
                  - 일정의 원본 ID
                - occurrenceDate (Query Parameter)
                  - 캘린더에서 사용자가 선택한 실제 발생 날짜
                  - 캘린더 조회 API 응답의 startTime 기준 날짜를 전달합니다.
    
                ### 응답 규칙
                - 단일 일정인 경우
                  → recurrenceGroup 필드는 null로 반환됩니다.
                - 반복 일정인 경우
                  → 반복 규칙 원본 정보(recurrenceGroup)를 함께 반환합니다.
    
                해당 API는 일정 수정 / 삭제 화면에서 사용됩니다.
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
            @PathVariable Long eventId,
            @Parameter(
                    description = "캘린더에서 선택한 실제 발생 날짜 (YYYY-MM-DD)",
                    example = "2026-02-06T14:00:00",
                    required = true
            )
            @RequestParam LocalDateTime occurrenceDate
    );

    @Operation(
            summary = "이벤트 목록 조회",
            description = """
        인증된 사용자의 이벤트를
        지정한 날짜 범위(startDate ~ endDate) 내에서 조회합니다.
        
        - 로그인 사용자 기준으로 조회됩니다.
        - 반복 일정은 지정한 기간 내에 포함되는 인스턴스만 반환됩니다.
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이벤트 목록 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 날짜 범위 요청"
            )
    })
    CustomResponse<EventResDTO.EventsListRes> getEvents(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @Parameter(
                    description = "조회 시작 날짜 (YYYY-MM-DD)",
                    example = "2026-01-01",
                    required = true
            )
            @RequestParam LocalDate startDate,

            @Parameter(
                    description = "조회 종료 날짜 (YYYY-MM-DD)",
                    example = "2026-01-31",
                    required = true
            )
            @RequestParam LocalDate endDate
    );

    @Operation(
            summary = "일정 수정",
            description = """
                선택한 일정을 수정합니다. (PATCH)
        
                이 API는 **부분 수정(PATCH)** 방식으로 동작하며,
                전달된 필드만 변경되고 나머지 필드는 유지됩니다.
        
                ---
                ## 공통 규칙
        
                - eventId는 **항상 필수**입니다.
                - 전달되지 않은 필드는 기존 값이 유지됩니다.
                - PATCH 요청이므로 값 비교가 아닌 **필드 존재 여부**로 변경 여부를 판단합니다.
                - 변경 의도가 없는 경우에도 기존 일정 정보를 그대로 반환합니다.
        
                ---
              
                - occurrenceDate는 **캘린더 화면에서 사용자가 선택한 실제 발생 일정의 날짜**입니다.
                - 반복 일정의 경우:
                  - occurrenceDate는 반복 규칙에 의해 **실제로 발생하는 날짜여야 합니다**.
                  - 반복 규칙에 존재하지 않는 날짜를 전달하면 오류가 발생합니다.
                    (예: 매달 15일 반복인데 14일 전달)
        
                - 단일 일정의 경우:
                  - occurrenceDate는 전달하지 않습니다.
        
                ---
                ## 반복 간격(intervalValue) 규칙
                
                - intervalValue는 간격(n일,n월,n년 마다)을 의미합니다.
                - 반복 규칙을 **변경하지 않는 경우**:
                  - intervalValue를 전달하지 않아도 됩니다.
                  - 기존 반복 그룹의 intervalValue가 유지됩니다.
                
                - 반복 규칙을 **변경하는 경우** (frequency 변경 또는 단일 일정에서 반복그룹(recurrenceGroup)을 생성):
                  - intervalValue을 1로 설정한다면 기본값이므로 전달하지 않아도 됩니다.
                
                ### frequency 별 intervalValue 허용 범위
                - DAILY   : 1 ~ 364
                - WEEKLY  : 1 (고정)
                - MONTHLY : 1 ~ 11
                - YEARLY  : 1 ~ 99
                
                ---
                ## 단일 일정 수정 (반복 없음)
        
                - recurrenceUpdateScope, recurrenceGroup을 전달하지 않습니다.
                - 전달된 필드만 단일 일정에 적용됩니다.
        
                ---
                ## 반복 일정 수정
        
                반복 일정인 경우 **recurrenceUpdateScope는 필수**입니다.
        
                ### 수정 범위 (recurrenceUpdateScope)
        
                #### THIS_EVENT
                - 선택한 occurrenceDate의 일정만 수정합니다.
                - 기존 반복 그룹에는 예외(RecurrenceException)가 추가됩니다.
                - 해당 일정은 반복 규칙에서 분리되지 않습니다.
        
                #### THIS_AND_FOLLOWING_EVENTS
                - 선택한 occurrenceDate과 이후의 일정들을 수정합니다.
                - 기존 반복 그룹은 occurrenceDate 이전까지만 유지됩니다.
                - 이후 일정들은 새로운 반복 그룹으로 재생성됩니다.
        
                #### ALL_EVENTS
                - 반복 일정 전체를 수정합니다.
                - 기존 반복 그룹과 실제 일정은 제거됩니다.
                - 새로운 반복 규칙으로 전체 일정이 재생성되고, 수정한 일정이 새 일정으로 생성됩니다.
        
                ---
                ## 시간(startTime / endTime) 처리 규칙
        
                - startTime 또는 endTime이 전달되면 해당 값으로 수정됩니다.
                - 시간 필드가 전달되지 않은 경우:
                  - occurrenceDate + 기존 일정의 시간 규칙으로 start/end가 재계산됩니다.
                - endTime이 전달되지 않은 경우:
                  - startTime + durationMinutes 기준으로 계산됩니다.
                - startTime,occurrenceDate 혹은 endTime,occurrenceDate 가 전달되지 않은 경우:
                  - eventId에 해당하는 db에 저장된 최초 일정의 startTime과 endTime을 사용합니다.
        
                ---
                ## 반복 규칙 수정 (recurrenceGroup)
        
                - 반복 규칙을 수정하는 경우에만 recurrenceGroup을 포함합니다.
                - recurrenceGroup 내부 필드 역시 **변경할 항목만 전달**합니다.
        
                ---
                ## 유효성 규칙
        
                - 반복이 없는 일정에 recurrenceUpdateScope, occurrenceDate를 지정하면 오류가 발생합니다.
                - 반복 일정인데 recurrenceUpdateScope가 없으면 오류가 발생합니다.
                - recurrenceGroup을 전달했는데 recurrenceUpdateScope가 없으면 오류가 발생합니다.
                - recurrenceGroup 필드가 frequency와 맞지 않으면 오류가 발생합니다.
                """
    )

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "일정 수정 요청 (PATCH)",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EventReqDTO.UpdateReq.class),
                    examples = {
                            // 1. 변경 없는 일정 수정
                            @ExampleObject(
                                    name = "변경 사항 없음",
                                    description = """
                                        변경 사항 없이 저장 버튼만 누른 경우.
                                        PATCH 요청이므로 body는 비어 있습니다.
                                        """,
                                    value = """
                                        {
                                        }
                                        """
                            ),


                            // 2-1. 반복 없는 일정 수정 (단일 일정)
                            @ExampleObject(
                                    name = "단일 일정 수정",
                                    description = """
                                        반복이 없는 단일 일정 수정.
                                        occurrenceDate는 전달하지 않습니다.
                                        """,
                                    value = """
                                        {
                                          "title": "팀 회의 (변경)",
                                          "location": "회의실 B"
                                        }
                                        """
                            ),
                            // 2-2. 반복 없는 일정에 반복 그룹 추가하는 수정 (단일일정 -> 반복 일정)
                            @ExampleObject(
                                    name = "단일 일정 - 반복 일정으로 변경",
                                    description = """
                                    반복이 없는 단일 일정을 반복 일정으로 변경합니다.
                            
                                    상황:
                                    - 반드시 반복이 없는 단일 일정(event)을 대상으로 해야 합니다.
                                    - 기존에 반복 그룹이 있는 일정에는 사용할 수 없습니다.
                                    - recurrenceGroup을 전달하므로 intervalValue는 필수입니다.
                                    """,
                                    value = """
                                        {
                                          "recurrenceGroup": {
                                            "frequency": "WEEKLY",
                                            "daysOfWeek": ["MON", "WED"],
                                            "endType": "NEVER"
                                          }
                                        }
                                        """
                            ),
                            // 3-1. 반복 일정 - 이 일정만 수정 (시간 변경)
                            @ExampleObject(
                                    name = "반복 일정 - 이 일정만 수정 (시간 변경)",
                                    description = """
                                        반복 일정 중 선택한 계산된 회차의 시간만 수정합니다.
                                        실제 eventId를 가진 일정을 수정하는 것이 아니라면 occurrenceDate는 필수입니다.
                                        """,
                                    value = """
                                        {
                                          "occurrenceDate": "2026-02-01",
                                          "startTime": "2026-02-06T14:00:00",
                                          "endTime": "2026-02-06T15:00:00",
                                          "recurrenceUpdateScope": "THIS_EVENT"
                                        }
                                        """
                            ),
                             // 3-2. 반복 일정 - 이 일정만 수정 (제목 변경)
                            @ExampleObject(
                                    name = "반복 일정 - 이 일정만 수정 (제목 변경)",
                                    description = """
                                        반복 일정 중 선택한 계산된 회차의 제목만 수정합니다.
                                        실제 eventId를 가진 일정을 수정하는 것이 아니라면 occurrenceDate는 필수입니다.
                                        """,
                                    value = """
                                        {
                                          "occurrenceDate": "2026-02-10",
                                          "title": "특별 회의",
                                          "recurrenceUpdateScope": "THIS_EVENT"
                                        }
                                        """
                            ),
                             // 4. 반복 일정 - 이 일정 + 이후 일정 수정 1
                            @ExampleObject(
                                    name = "반복 일정 - 이 일정 + 이후 수정",
                                    description = """
                                    선택한 회차와 그 이후 일정들의 반복 규칙을 수정합니다.
                                    """,
                                    value = """
                                        {
                                          "occurrenceDate": "2026-02-06",
                                          "recurrenceUpdateScope": "THIS_AND_FOLLOWING_EVENTS",
                                          "recurrenceGroup": {
                                            "frequency": "WEEKLY",
                                            "daysOfWeek": ["THU"],
                                            "endType": "NEVER"
                                          }
                                        }
                                        """
                            ),
                            // 5. 반복 일정 - 이 일정 + 이후 일정 수정 2
                            @ExampleObject(
                                    name = "반복 일정 - 이 일정 + 이후 수정 (intervalValue 포함)",
                                    description = """
                                    선택한 회차와 그 이후 일정들의 반복 규칙을 수정합니다.
                            
                                    상황:
                                    - 반복 타입이 WEEKLY가 아닌 다른 타입을 가진 일정을 대상으로 반복 객체를 수정하는 상황입니다.
                                    """,
                                    value = """
                                        {
                                          "occurrenceDate": "2026-02-06",
                                          "recurrenceUpdateScope": "THIS_AND_FOLLOWING_EVENTS",
                                          "recurrenceGroup": {
                                            "frequency": "WEEKLY",
                                            "daysOfWeek": ["MON", "THU"],
                                            "endType": "NEVER"
                                          }
                                        }
                                        """
                            ),

                            // 6. 반복 일정 - 전체 수정 1
                            @ExampleObject(
                                    name = "반복 일정 - 전체 수정",
                                    description = """
                                    반복 일정 전체의 반복 규칙을 수정합니다.
                                    """,
                                    value = """
                                        {
                                          "recurrenceUpdateScope": "ALL_EVENTS",
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
                            // 7. 반복 일정 - 전체 수정 2
                            @ExampleObject(
                                    name = "반복 일정 - 전체 수정 (intervalValue 포함)",
                                    description = """
                                    반복 일정 전체의 반복 규칙을 수정합니다.
                            
                                    상황:
                                    - 반복 타입이 YEARLY가 아닌 같은 타입을 가진 일정을 대상으로 반복 객체를 수정하는 상황입니다.
                                    """,
                                    value = """
                                        {
                                          "recurrenceUpdateScope": "ALL_EVENTS",
                                          "recurrenceGroup": {
                                            "frequency": "YEARLY",
                                            "intervalValue": 2
                                          }
                                        }
                                        """
                            )

                    }
            )
    )
    @ApiResponses({

            // =======================
            // 200 OK
            // =======================
            @ApiResponse(
                    responseCode = "200",
                    description = "일정 수정 성공",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "일정 수정 성공",
                                    value = """
                            {
                              "isSuccess": true,
                              "code": "200",
                              "message": "수정 완료",
                              "result": null
                            }
                            """
                            )
                    )
            ),

            // =======================
            // 400 BAD REQUEST
            // =======================
            @ApiResponse(
                    responseCode = "400",
                    description = "일정 수정 요청이 유효성 규칙을 위반한 경우",
                    content = @Content(
                            examples = {
                                    // EVENT
                                    @ExampleObject(
                                            name = "EVENT400_3",
                                            summary = "반복이 없는 일정인데 수정 범위가 지정된 경우",
                                            value = """
                                    {
                                      "isSuccess": false,
                                      "code": "EVENT400_3",
                                      "message": "반복이 없는 일정입니다."
                                    }
                                    """
                                    ),

                                    @ExampleObject(
                                            name = "EVENT400_4",
                                            summary = "반복이 없는데 occurrenceDate가 전달된 경우",
                                            value = """
                                    {
                                      "isSuccess": false,
                                      "code": "EVENT400_4",
                                      "message": "반복이 없는 일정입니다."
                                    }
                                    """
                                    ),

                                    @ExampleObject(
                                            name = "EVENT400_5",
                                            summary = "반복 일정인데 occurrenceDate가 전달되지 않은 경우\n" +
                                                    "즉,사용자가 선택한 일정이 계산되지 않은 실제 일정일 경우(DB에 저장된 원본 일정)" +
                                                    " 반복 방식 변경시 RecurrenceUpdateScope은 모든 이벤트에 적용만 가능하다\n" +
                                                    "계산된 일정이 아닌, 실제 일정을 수정할때는 occurrenceDate에 값이 없다," +
                                                    "occurrenceDate에 필드는 계산된 일정의 startDate를 띄우기 때문이다.\n" +
                                                    "즉, 실제 일정을 수정 시, RecurrenceUpdateScope이 모든 이벤트에 대한 경우가 " +
                                                    "아니라면, 이 요청은 반복은 존재하지만 occurrenceDate가 없는 잘못된 요청이다. ",
                                            value = """
                                    {
                                      "isSuccess": false,
                                      "code": "EVENT400_5",
                                      "message": "OCCURRENCE_DATE가 없습니다."
                                    }
                                    """
                                    ),

                                    // RECURRENCE GROUP

                                    @ExampleObject(
                                            name = "RG400_8",
                                            summary = "매달 반복 주가 설정되지 않은 경우\n" +
                                                    "- frequency : MONTHLY, monthlyType : DAY_OF_WEEK인데" +
                                                    "weekOfMonth 필드가 Null 인경우",
                                            value = """
                                    {
                                      "isSuccess": false,
                                      "code": "RG400_8",
                                      "message": "매달 반복 주가 설정되지 않았습니다."
                                    }
                                    """
                                    ),

                                    @ExampleObject(
                                            name = "RG400_9",
                                            summary = "그 달의 n번째 주 요일이 설정되지 않은 경우\n" +
                                                    "- frequency : MONTHLY, monthlyType : DAY_OF_WEEK인데" +
                                                    "dayOfWeekInMonth 필드가 Null 인경우",
                                            value = """
                                    {
                                      "isSuccess": false,
                                      "code": "RG400_9",
                                      "message": "그 달의 n번째주 요일이 설정되지 않았습니다."
                                    }
                                    """
                                    ),

                                    @ExampleObject(
                                            name = "RG400_15",
                                            summary = "반복 타입에 맞지 않는 필드가 함께 전달된 경우\n" +
                                                    "- frequency : YEARLY인데 dayOfWeek에 값이 있는 경우",
                                            value = """
                                    {
                                      "isSuccess": false,
                                      "code": "RG400_15",
                                      "message": "FREQUENCY 타입에 따른 불필요한 필드값이 채워져 있습니다."
                                    }
                                    """
                                    ),

                                    @ExampleObject(
                                            name = "RG400_17",
                                            summary = "반복 간격 값 범위가 올바르지 않은 경우\n" +
                                                    "반복 타입에 따른 intervalValue 범위를 벗어난 경우",
                                            value = """
                                    {
                                      "isSuccess": false,
                                      "code": "RG400_17",
                                      "message": "간격 값 범위가 올바르지 않습니다."
                                    }
                                    """
                                    )
                            }
                    )
            ),

            // =======================
            // 404 NOT FOUND
            // =======================
            @ApiResponse(
                    responseCode = "404",
                    description = "일정을 찾을 수 없는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "EVENT404_1",
                                    summary = "일정 ID가 존재하지 않음",
                                    value = """
                            {
                              "isSuccess": false,
                              "code": "EVENT404_1",
                              "message": "일정을 찾을 수 없습니다"
                            }
                            """
                            )
                    )
            )
    })
    CustomResponse<EventResDTO.DetailRes> updateEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,

            @Parameter(
                    description = "수정할 일정 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long eventId,

            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "일정 수정 요청 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = EventReqDTO.UpdateReq.class)
                    )
            )
            EventReqDTO.UpdateReq req
    );

}
