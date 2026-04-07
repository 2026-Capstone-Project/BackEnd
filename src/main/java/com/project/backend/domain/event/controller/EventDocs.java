package com.project.backend.domain.event.controller;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.dto.response.swagger.EventDetailRes;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
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
import java.time.LocalDateTime;

@Tag(name = "Event API", description = "일정 API")
public interface EventDocs {

    @Operation(
            summary = "일정 생성",
            description = """
                새로운 일정을 생성합니다.
                                
                ## 일정(Event) 필수 파라미터
                - title (String) : 일정 제목
                - startTime (LocalDateTime) : 일정 시작 일시
                - endTime (LocalDateTime) : 일정 종료 일시
                                
                ## 일정(Event) 선택 파라미터
                - content (String) : 일정 메모
                - location (String) : 장소
                - address (String) : location 주소
                - isAllDay (Boolean) : 종일 여부 (미전송 시 false)
                - friendIds (List<Long>) : 공유할 member와의 친구 id
                - color (EventColor) : 색상 (미전송 시 기본값 적용) [BLUE(기본값), GREEN, PINK, PURPLE, GRAY, YELLOW]
                - recurrenceGroup (RecurrenceGroup) : 반복 설정 (없으면 단일 일정)
                                
                ---
                ## 🔄 반복 설정(recurrenceGroup) 입력 규칙
                                
                - 반복을 사용하지 않는 경우 → recurrenceGroup 필드는 **아예 보내지 않습니다**
                - 반복을 사용하는 경우에만 → recurrenceGroup 객체를 포함합니다
                                
                ---
                ## 🔁 반복 간격(intervalValue) 규칙
                - intervalValue는 간격(n일/n월/n년마다)을 의미합니다.
                - 반복 규칙을 생성하는 경우 intervalValue=1은 기본값이므로 생략 가능합니다.
                                
                ### frequency 별 intervalValue 허용 범위
                - DAILY   : 1 ~ 364
                - WEEKLY  : 1 (고정)
                - MONTHLY : 1 ~ 11
                - YEARLY  : 1 ~ 99
                                
                ---
                ## 📌 recurrenceGroup 필드 (CreateReq 기준)
                                
                ### 공통 필드
                - frequency (RecurrenceFrequency)
                  - DAILY / WEEKLY / MONTHLY / YEARLY
                                
                - intervalValue (Integer)
                  - 기본값 1 (생략 가능)
                                
                - endType (RecurrenceEndType)
                  - NEVER / END_BY_DATE / END_BY_COUNT
                  - null로 보내면 NEVER로 저장됩니다.
                  - endType이 null인 경우 endDate, occurrenceCount도 반드시 null이어야 합니다.
                                
                - endDate (LocalDate) 조건부
                  - endType = END_BY_DATE 일 때 필수
                                
                - occurrenceCount (Integer) 조건부
                  - endType = END_BY_COUNT 일 때 필수 (1 이상)
                                
                ---
                ### DAILY (매일 반복)
                - 추가 필드 없음
                                
                ---
                ### WEEKLY (매주 반복)
                - daysOfWeek (List<DayOfWeek>)
                  - 예: ["MONDAY", "WEDNESDAY", "FRIDAY"]
                  - null로 보내면 일정의 startTime 기준 요일로 자동 설정됩니다.
                                
                ---
                ### MONTHLY (매월 반복)
                - monthlyType (MonthlyType)
                  - DAY_OF_MONTH : 매월 N일
                  - DAY_OF_WEEK  : 매월 N번째 X요일
                  - null로 보내면 DAY_OF_MONTH로 저장됩니다.
                                
                #### monthlyType = DAY_OF_MONTH (매월 N일)
                - daysOfMonth (List<Integer>)
                  - 1~31
                  - null이면 startTime 기준 '일'로 자동 설정됩니다.
                  - 예: [15], [15, 30]
                                
                #### monthlyType = DAY_OF_WEEK (매월 N번째 X요일)
                - weekOfMonth (Integer)
                  - 1~5
                  - null이면 startTime 기준 주차로 자동 설정됩니다.
                                
                - weekdayRule (MonthlyWeekdayRule)
                  - SINGLE / WEEKDAY / WEEKEND / ALL_DAYS
                  - null로 보내면 SINGLE로 저장됩니다.
                                
                - dayOfWeekInMonth (DayOfWeek)
                  - 예: "TUESDAY"
                  - **요일은 하나만 입력 가능합니다.**
                  - null이면 startTime 기준 요일로 자동 설정됩니다.
                                
                **일정 생성, 수정 시 weekdayRule 사용법**
                                
                **단일 요일**
                - 단일 요일 선택 시에는 weekdayRule = SINGLE, dayOfWeekInMonth = "MONDAY" 입니다.
                - weekdayRule = SINGLE만 보내면, dayOfWeekInMonth 값은 해당 일정의 startTime의 요일로 초기화됩니다.
                - dayOfWeekInMonth = "MONDAY"만 보내면, weekdayRule 값은 무조건 SINGLE로 초기화됩니다.
                                
                **평일, 주말, 1주 전체**
                - weekdayRule = WEEKDAY or WEEKEND or ALL_DAYS 이면 dayOfWeekInMonth = null 입니다.
                                
                ---
                ### YEARLY (매년 반복)
                - monthOfYear (Integer)
                  - 1~12
                  - null이면 startTime 기준 월로 자동 설정됩니다.
                                
                ---
                ## ✅ 반복 일정 생성 규칙 (규칙 필드 우선 + 기본값은 startTime)
                                
                - startTime은 생성 시 필수입니다.
                - 반복 관련 필드(요일/일/주/월)가 **전달된 경우**, 해당 값이 **우선 적용**되며 startTime은 그 값에 맞춰 **보정하지 않습니다.**
                - 반복 관련 필드가 **비어 있거나 누락된 경우에만**, startTime을 기준으로 기본값이 자동 채워집니다.
                  - WEEKLY: daysOfWeek가 없으면 startTime의 요일 1개로 설정
                  - MONTHLY(DAY_OF_MONTH): daysOfMonth가 없으면 startTime의 일로 설정
                  - MONTHLY(DAY_OF_WEEK): weekOfMonth / dayOfWeekInMonth / weekdayRule이 없으면 startTime 기준으로 설정
                  - YEARLY: monthOfYear가 없으면 startTime의 월로 설정
                """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "일정 생성 요청",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EventReqDTO.CreateReq.class),
                    examples = {
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
                                              "address": "서울특별시 종로구 홍지문2길 20",
                                              "color": "BLUE",
                                              "isAllDay": false,
                                              "friendIds": []
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "매일 반복 일정 (최소 입력)",
                                    description = "frequency만 DAILY로 설정한 기본 매일 반복",
                                    value = """
                                            {
                                              "title": "일일 스탠드업",
                                              "startTime": "2026-01-27T09:00:00",
                                              "endTime": "2026-01-27T09:15:00",
                                              "recurrenceGroup": {
                                                "frequency": "DAILY"
                                              }
                                            }
                                            """
                            ),
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
                                                "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
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
                                                "frequency": "WEEKLY"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "월간 반복 일정 (매월 N일)",
                                    description = "매월 15일 반복",
                                    value = """
                                            {
                                              "title": "월급날 확인",
                                              "startTime": "2026-01-15T09:00:00",
                                              "endTime": "2026-01-15T09:30:00",
                                              "location": "회의실 A",
                                              "address": "서울특별시 종로구 홍지문2길 20",
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
                                    description = "매월 2번째 화요일",
                                    value = """
                                            {
                                              "title": "월간 점검",
                                              "startTime": "2026-01-15T09:00:00",
                                              "endTime": "2026-01-15T09:30:00",
                                              "color": "GREEN",
                                              "recurrenceGroup": {
                                                "frequency": "MONTHLY",
                                                "monthlyType": "DAY_OF_WEEK",
                                                "weekOfMonth": 2,
                                                "dayOfWeekInMonth": "TUESDAY"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "월간 반복 일정 (매월 N일, 최소 입력, 2개월마다 반복)",
                                    description = "monthlyType DAY_OF_MONTH, 날짜는 startTime 기준 자동 설정",
                                    value = """
                                            {
                                              "title": "월간 정산",
                                              "startTime": "2026-01-15T09:00:00",
                                              "endTime": "2026-01-15T09:30:00",
                                              "recurrenceGroup": {
                                                "frequency": "MONTHLY",
                                                "intervalValue": 2,
                                                "monthlyType": "DAY_OF_MONTH"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "월간 반복 일정 (매월 N번째 요일, 최소 입력, 3개월마다 반복)",
                                    description = "monthlyType DAY_OF_WEEK, 주차/요일은 startTime 기준 자동 설정",
                                    value = """
                                            {
                                              "title": "월간 회의",
                                              "startTime": "2026-01-27T14:00:00",
                                              "endTime": "2026-01-27T15:00:00",
                                              "recurrenceGroup": {
                                                "frequency": "MONTHLY",
                                                "intervalValue": 3,
                                                "monthlyType": "DAY_OF_WEEK",
                                                "weekOfMonth": 1,
                                                "weekdayRule": "WEEKDAY"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "연간 반복 일정",
                                    description = "매년 6월 반복",
                                    value = """
                                            {
                                              "title": "기념일",
                                              "startTime": "2026-01-15T09:00:00",
                                              "endTime": "2026-01-15T09:30:00",
                                              "location": "회의실 A",
                                              "address": "서울특별시 종로구 홍지문2길 20",
                                              "recurrenceGroup": {
                                                "frequency": "YEARLY",
                                                "monthOfYear": 6,
                                                "endType": "END_BY_COUNT",
                                                "occurrenceCount": 9
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "연간 반복 일정 (최소 입력, 2년마다 반복)",
                                    description = "frequency만 YEARLY로 설정, 월/일은 startTime 기준 자동 설정",
                                    value = """
                                            {
                                              "title": "기념일",
                                              "startTime": "2026-01-15T09:00:00",
                                              "endTime": "2026-01-15T09:30:00",
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
            @ApiResponse(
                    responseCode = "200",
                    description = "일정 생성 성공",
                    content = @Content(
                            schema = @Schema(implementation = EventResDTO.CreateRes.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "일정 생성 요청이 유효성 규칙을 위반한 경우",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "EVENT400_1",
                                            summary = "시간 필드를 설정하지 않은 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_1",
                                                      "message": "시간을 설정하지 않았습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_2",
                                            summary = "end 시간이 start 시간보다 이전인 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_2",
                                                      "message": "시간 설정이 잘못되었습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_3_INVALID_TITLE",
                                            summary = "제목에 공백만 입력한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_3",
                                                      "message": "제목에는 공백만 입력할 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_4_INVALID_LOCATION",
                                            summary = "위치에 공백만 입력한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_4",
                                                      "message": "위치에는 공백만 입력할 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_5_INVALID_ADDRESS",
                                            summary = "주소에 공백만 입력한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_5",
                                                      "message": "주소에는 공백만 입력할 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_11",
                                            summary = "자기 자신을 초대한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_11",
                                                      "message": "자기자신은 초대할 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "RG400_1",
                                            summary = "설정한 종료 타입과 관련 없는 필드가 함께 전달된 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "RG400_1",
                                                      "message": "EndType 타입에 따른 불필요한 필드값이 채워져 있습니다"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "RG400_2",
                                            summary = "EndType이 END_BY_DATE인데 endDate가 없는 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "RG400_2",
                                                      "message": "종료 날짜가 설정되지 않았습니다"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "RG400_3",
                                            summary = "EndType이 END_BY_COUNT인데 occurrenceCount가 없는 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "RG400_3",
                                                      "message": "종료 카운트가 설정되지 않았습니다"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "RG400_4_INVALID_END_TYPE",
                                            summary = "유효하지 않은 EndType 값이 전달된 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "RG400_4",
                                                      "message": "잘못된 종료타입입니다"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "RG400_9_INVALID_WEEKDAY_RULE_COMBINATION",
                                            summary = "weekdayRule이 SINGLE이 아닌데 개별 요일을 함께 선택한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "RG400_9",
                                                      "message": "주중, 주말, 모든 날 선택 시 개별 요일 선택을 사용할 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "RG400_13",
                                            summary = "endDate가 일정 시작 날짜보다 이전인 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "RG400_13",
                                                      "message": "종료 날짜가 일정 시작 날짜보다 빠릅니다"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "RG400_14",
                                            summary = "유효하지 않은 요일 값이 전달된 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "RG400_14",
                                                      "message": "잘못된 요일입니다"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "RG400_15",
                                            summary = "설정한 반복 타입과 관련 없는 필드가 함께 전달된 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "RG400_15",
                                                      "message": "FREQUENCY 타입에 따른 불필요한 필드값이 채워져 있습니다"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "생성 대상이나 초대 대상이 유효하지 않은 경우",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "EVENT404_5",
                                            summary = "초대하려는 대상이 주최자와 친구가 아닌 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT404_5",
                                                      "message": "주최자와 친구사이가 아닙니다."
                                                    }
                                                    """
                                    )
                            }
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
                      - ✅ 조회에 사용되는 **태생적 발생일시(LocalDateTime)** 입니다.
                      - 캘린더 목록 응답에서 제공되는 **occurrenceDate 값을 그대로 전달**합니다.
                      - ⚠️ UI에 표시되는 start(수정 반영된 실제 시간)를 전달하면 조회에 실패할 수 있습니다.
                      - 형식: YYYY-MM-DDTHH:mm:ss
                    
                    ### 응답 규칙
                    - 단일 일정인 경우 → recurrenceGroup은 null로 반환됩니다.
                    - 반복 일정인 경우 → recurrenceGroup(원본 반복 규칙 정보)을 함께 반환합니다.
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
                                    name = "EVENT404_1",
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
    CustomResponse<EventResDTO.DetailRes> getEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,

            @Parameter(
                    description = "조회할 일정 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long eventId,

            @Parameter(
                    description = "캘린더에서 선택한 일정의 태생적 날짜 (YYYY-MM-DDTHH:mm:ss)",
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
            summary = "일정 제목 검색 기록 조회",
            description = """
                    인증된 사용자의 일정 제목 검색 기록을 조회합니다.
                    
                    - keyword가 없거나 공백이면 최근 일정 제목 기록 5개를 조회합니다.
                    - keyword가 있으면 일치하는 일정 제목 기록 중 최대 5개를 조회합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "일정 제목 검색 기록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = EventResDTO.EventTitleHistoryRes.class)
                    )
            )
    })
    CustomResponse<EventResDTO.EventTitleHistoryRes> getEventTitleHistory(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @Parameter(
                    description = "검색 키워드. 없거나 공백이면 최근 5개, 있으면 일치하는 제목 기록 중 최대 5개를 조회합니다.",
                    required = false
            )
            @RequestParam(value = "keyword", required = false) String keyword
    );

    @Operation(
            summary = "일정 수정",
            description = """
                    선택한 일정을 수정합니다. (PATCH)
                    
                    이 API는 **부분 수정(PATCH)** 방식으로 동작하며,
                    전달된 필드만 변경되고 나머지 필드는 유지됩니다.
                    
                    ---
                    ## ✅ 공통 규칙
                    
                    - eventId는 **항상 필수**입니다.
                    - occurrenceDate는 **항상 필수**입니다.
                    - 전달되지 않은 필드는 기존 값이 유지됩니다.
                    - PATCH 요청이므로 값 비교가 아닌 **필드 존재 여부**로 변경 여부를 판단합니다.
                    - 변경 의도가 없는 경우에도 기존 일정 정보를 그대로 반환할 수 있습니다.
                    
                    ---
                    ## 📌 occurrenceDate 규칙
                    
                    - occurrenceDate는 **UI에 보이는 start(수정 반영된 실제 시간)가 아닙니다.**
                    - ✅ occurrenceDate는 일정마다 고정되는 **태생적 발생일시(LocalDateTime)** 입니다.
                    - 캘린더 목록/상세 조회 응답에 포함된 **occurrenceDate 값을 그대로 전달**해야 합니다.
                    - 형식: YYYY-MM-DDTHH:mm:ss
                    
                    ---
                    ## 🔁 반복 일정 수정 (scope)
                    
                    - 단일 일정인 경우 scope는 필요하지 않습니다.
                    - 반복 일정인 경우 scope는 필수입니다.
                    - 사용 가능 값:
                      - THIS_EVENT
                      - THIS_AND_FOLLOWING_EVENTS
                    
                    ### ✅ THIS_EVENT
                    - 선택한 occurrenceDate(태생) 회차만 수정합니다.
                    - RecurrenceException(OVERRIDE)이 생성/갱신됩니다.
                    - **이 범위에서는 friendIds로 공유 참여자 수정이 불가능합니다.**
                    
                    ### ✅ THIS_AND_FOLLOWING_EVENTS
                    - 선택한 occurrenceDate(태생) 회차와 그 이후를 수정합니다.
                    - 기존 반복 그룹은 occurrenceDate 이전까지만 유지됩니다.
                    - occurrenceDate는 새 반복의 기준점(base)이 됩니다.
                    - 반복 일정을 대상으로 반복 관련 필드를 수정하는 경우 THIS_AND_FOLLOWING_EVENTS를 통해서만 수정 가능합니다.
                    
                    ---
                    ## ⏱️ 시간(startTime / endTime) 처리 규칙
                    
                    - startTime 또는 endTime이 전달되면 해당 값으로 수정됩니다.
                    
                    - startTime이 전달되지 않은 경우:
                      - 선택한 occurrenceDate의 날짜로 보정됩니다.
                    
                    - endTime이 전달되지 않은 경우:
                      - 보정된 startTime과 기존 duration 규칙을 기준으로 자동 계산됩니다.
                    
                    ---
                    ## 🔄 반복 규칙 수정 (recurrenceGroup)
                    
                    - 반복 규칙을 수정하는 경우에만 recurrenceGroup을 포함합니다.
                    - recurrenceGroup 내부 필드 역시 **변경할 항목만 전달**합니다.
                    
                    ---
                    ## ⚠️ 유효성 규칙
                    
                    - 반복 일정인데 scope가 없으면 오류가 발생합니다.
                    - recurrenceGroup을 전달했는데 scope가 없으면 오류가 발생합니다.
                    - recurrenceGroup 필드가 frequency와 맞지 않으면 오류가 발생합니다.
                    - title은 공백만 입력할 수 없습니다.
                    - location은 공백만 입력할 수 없습니다.
                    - address는 공백만 입력할 수 없습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "일정 수정 요청 (PATCH)",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = EventReqDTO.UpdateReq.class),
                    examples = {
                            @ExampleObject(
                                    name = "변경 사항 없음",
                                    description = "변경 사항 없이 저장 버튼만 누른 경우. PATCH 요청이므로 body는 비어 있습니다.",
                                    value = """
                                            {
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "단일 일정 수정",
                                    value = """
                                            {
                                              "title": "팀 회의 (변경)",
                                              "location": "회의실 B",
                                              "address": "서울특별시 종로구 홍지문2길 20"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "단일 일정 - 반복 일정으로 변경",
                                    description = """
                                            반복이 없는 단일 일정을 반복 일정으로 변경합니다.
                                            
                                            상황:
                                            - 반드시 반복이 없는 단일 일정(event)을 대상으로 해야 합니다.
                                            - 기존에 반복 그룹이 있는 일정에는 사용할 수 없습니다.
                                            """,
                                    value = """
                                            {
                                              "recurrenceGroup": {
                                                "frequency": "WEEKLY",
                                                "daysOfWeek": ["MONDAY", "WEDNESDAY"],
                                                "endType": "NEVER"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "반복 일정 - 이 일정만 수정 (시간 변경)",
                                    description = "반복 일정 중 선택한 회차의 시간만 수정합니다.",
                                    value = """
                                            {
                                              "startTime": "2026-02-06T14:00:00",
                                              "endTime": "2026-02-06T15:00:00"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "반복 일정 - 이 일정만 수정 (제목 변경)",
                                    description = "반복 일정 중 선택한 회차의 제목만 수정합니다.",
                                    value = """
                                            {
                                              "title": "특별 회의"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "반복 일정 - 이 일정 + 이후 수정",
                                    description = "선택한 회차와 그 이후 일정들의 반복 규칙을 수정합니다.",
                                    value = """
                                            {
                                              "recurrenceGroup": {
                                                "frequency": "WEEKLY",
                                                "daysOfWeek": ["THURSDAY"],
                                                "endType": "NEVER"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "반복 일정 - 이 일정 + 이후 수정 (참여자 포함)",
                                    description = "선택한 회차와 그 이후 일정들에 대해 공유 참여자까지 함께 수정합니다.",
                                    value = """
                                            {
                                              "friendIds": [3, 7],
                                              "recurrenceGroup": {
                                                "frequency": "WEEKLY",
                                                "daysOfWeek": ["MONDAY", "THURSDAY"],
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
            @ApiResponse(
                    responseCode = "400",
                    description = "일정 수정 요청이 유효성 규칙을 위반한 경우",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "EVENT400_3_SCOPE_NOT_REQUIRED",
                                            summary = "단일 일정인데 scope가 전달된 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_3",
                                                      "message": "UPDATE_SCOPE 설정이 필요하지 않습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_3_INVALID_TITLE",
                                            summary = "제목에 공백만 입력한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_3",
                                                      "message": "제목에는 공백만 입력할 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_4_INVALID_LOCATION",
                                            summary = "위치에 공백만 입력한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_4",
                                                      "message": "위치에는 공백만 입력할 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_5_OCCURRENCE_REQUIRED",
                                            summary = "반복 일정인데 occurrenceDate가 전달되지 않은 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_5",
                                                      "message": "OCCURRENCE_DATE가 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_5_INVALID_ADDRESS",
                                            summary = "주소에 공백만 입력한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_5",
                                                      "message": "주소에는 공백만 입력할 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_7",
                                            summary = "반복 일정인데 scope가 전달되지 않은 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_7",
                                                      "message": "UPDATE_SCOPE가 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_8",
                                            summary = "유효하지 않은 scope 값",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_8",
                                                      "message": "존재하지 않는UPDATE_SCOPE 값입니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_10",
                                            summary = "반복 필드 수정 또는 단일 일정 -> 반복 일정 변경인데 THIS_AND_FOLLOWING_EVENTS가 아닌 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_10",
                                                      "message": "반복 필드 수정, 단일 일정 -> 반복 일정으로 수정 시, THIS_AND_FOLLOWING_EVENTS만 가능합니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_11",
                                            summary = "자기 자신을 공유 대상으로 전달한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_11",
                                                      "message": "자기자신은 초대할 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_12",
                                            summary = "THIS_EVENT 수정에서 friendIds를 함께 전달한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_12",
                                                      "message": "ThisEvent일 때는 공유 불가."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "RG400_8",
                                            summary = "매달 반복 주가 설정되지 않은 경우",
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
                                            summary = "그 달의 n번째 주 요일이 설정되지 않은 경우",
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
                                            summary = "반복 타입에 맞지 않는 필드가 함께 전달된 경우",
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
                                            summary = "반복 간격 값 범위가 올바르지 않은 경우",
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
            @ApiResponse(
                    responseCode = "404",
                    description = "일정을 찾을 수 없거나 공유 대상이 유효하지 않은 경우",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "EVENT404_1",
                                            summary = "일정 ID가 존재하지 않음",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT404_1",
                                                      "message": "일정을 찾을 수 없습니다"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT404_5",
                                            summary = "공유 대상으로 전달한 friendIds가 주최자와 친구 관계가 아닌 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT404_5",
                                                      "message": "주최자와 친구사이가 아닙니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    CustomResponse<Void> updateEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,

            @Parameter(
                    description = "수정할 일정 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long eventId,

            @Parameter(
                    description = "캘린더에서 선택한 일정의 태생적 날짜 (YYYY-MM-DDTHH:mm:ss)",
                    example = "2026-02-06T14:00:00",
                    required = true
            )
            @RequestParam LocalDateTime occurrenceDate,

            @Parameter(
                    description = "반복 일정 수정 범위 (반복 일정인 경우 필수)",
                    schema = @Schema(allowableValues = {"THIS_EVENT", "THIS_AND_FOLLOWING_EVENTS"}),
                    required = false
            )
            @RequestParam(required = false) RecurrenceUpdateScope scope,

            @RequestBody EventReqDTO.UpdateReq req
    );

    @Operation(
            summary = "공유 이벤트 탈퇴",
            description = """
                    현재 로그인한 사용자가 공유받은 이벤트에서 탈퇴합니다.
                    
                    ### 요청 규칙
                    - eventId는 탈퇴할 공유 이벤트의 ID입니다.
                    - 이벤트 소유자는 이 API를 호출할 수 없습니다.
                    - 현재 사용자가 해당 이벤트의 참여자가 아니면 탈퇴할 수 없습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공유 이벤트 탈퇴 성공",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "200",
                                              "message": "이벤트 공유 탈퇴 완료",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "탈퇴 권한이 없는 경우",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "EVENT403_2",
                                            summary = "이벤트 소유자가 탈퇴를 시도한 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT403_2",
                                                      "message": "이벤트 소유자는 이벤트를 떠날 수 없습니다"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "이벤트 또는 참여 정보를 찾을 수 없는 경우",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "EVENT404_1",
                                            summary = "이벤트가 존재하지 않는 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT404_1",
                                                      "message": "일정을 찾을 수 없습니다"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT404_6",
                                            summary = "해당 사용자의 공유 참여 정보가 없는 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT404_6",
                                                      "message": "해당 일정 공유 초대가 존재하지 않습니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    CustomResponse<String> leaveSharedEvent(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @Parameter(
                    description = "탈퇴할 공유 이벤트 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long eventId
    );

    @Operation(
            summary = "일정 삭제",
            description = """
                    선택한 일정을 삭제합니다.
                    
                    ---
                    ## ✅ 변경사항
                    - 삭제 기준 날짜 타입은 LocalDate가 아닌 **LocalDateTime(초 포함)** 입니다.
                    
                    ---
                    ## 🧾 요청 파라미터
                    
                    ### Path Variable
                    - eventId (필수) : 삭제할 일정 ID
                    
                    ### Query Parameters
                    - occurrenceDate (필수)
                      - ✅ 삭제에 사용되는 **태생적 발생일시(LocalDateTime)** 입니다.
                      - 캘린더 목록/상세 조회 응답의 **occurrenceDate 값을 그대로 전달**합니다.
                      - ⚠️ UI에 표시되는 start(수정 반영된 실제 시간)를 전달하면 삭제에 실패할 수 있습니다.
                      - 형식: YYYY-MM-DDTHH:mm:ss
                    
                    - scope (선택/조건부)
                      - 반복 일정 삭제 범위
                      - 사용 가능 값:
                        - THIS_EVENT
                        - THIS_AND_FOLLOWING_EVENTS
                    
                    ---
                    ## 🗑️ 삭제 시나리오
                    
                    ### THIS_EVENT
                    - 선택한 occurrenceDate(태생) 회차만 삭제됩니다.
                    - 반복 일정: RecurrenceException(SKIP) 처리됩니다.
                    - 단일 일정: 즉시 삭제됩니다.
                    
                    ### THIS_AND_FOLLOWING_EVENTS
                    - 선택한 occurrenceDate(태생) 회차와 그 이후 일정이 삭제됩니다.
                    - 기존 반복 그룹은 occurrenceDate 이전까지만 유지됩니다.
                    
                    ---
                    ## ⚠️ 유효성 규칙
                    - 단일 일정인데 scope가 전달되면 오류가 발생할 수 있습니다.
                    - 반복 일정인데 scope가 없으면 오류가 발생합니다.
                    - occurrenceDate가 없으면 오류가 발생합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "일정 삭제 성공",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    summary = "삭제 성공",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "200",
                                              "message": "삭제 완료",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "일정 삭제 요청이 유효성 규칙을 위반한 경우",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "EVENT400_3_SCOPE_NOT_REQUIRED",
                                            summary = "단일 일정인데 scope가 전달된 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_3",
                                                      "message": "UPDATE_SCOPE 설정이 필요하지 않습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_5_OCCURRENCE_REQUIRED",
                                            summary = "반복 일정인데 occurrenceDate가 없는 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_5",
                                                      "message": "OCCURRENCE_DATE가 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_7",
                                            summary = "반복 일정인데 scope가 없는 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_7",
                                                      "message": "UPDATE_SCOPE가 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "EVENT400_8",
                                            summary = "유효하지 않은 scope 값",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "EVENT400_8",
                                                      "message": "존재하지 않는UPDATE_SCOPE 값입니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
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
    CustomResponse<Void> deleteEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,

            @Parameter(
                    description = "삭제할 일정 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long eventId,

            @Parameter(
                    description = "캘린더에서 선택한 일정의 태생적 날짜 (YYYY-MM-DDTHH:mm:ss)",
                    example = "2026-02-06T14:00:00",
                    required = true
            )
            @RequestParam LocalDateTime occurrenceDate,

            @Parameter(
                    description = "반복 일정 삭제 범위",
                    example = "THIS_EVENT",
                    required = false
            )
            @RequestParam(required = false) RecurrenceUpdateScope scope
    );

    @Operation(
            summary = "이벤트 참여자 전체 삭제",
            description = """
                    현재 로그인한 사용자가 소유한 이벤트의 모든 참여자를 삭제합니다.
                    
                    ### 요청 규칙
                    - eventId는 참여자를 삭제할 이벤트의 ID입니다.
                    - 이벤트 소유자만 호출할 수 있습니다.
                    - 이벤트가 존재하지 않거나 소유자가 아니면 삭제할 수 없습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이벤트 참여자 전체 삭제 성공",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "SUCCESS",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "200",
                                              "message": "삭제 완료",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "이벤트를 찾을 수 없는 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "EVENT404_1",
                                    summary = "이벤트가 존재하지 않거나 현재 사용자가 소유자가 아닌 경우",
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
    CustomResponse<Void> deleteEventParticipants(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @Parameter(
                    description = "참여자를 전체 삭제할 이벤트 ID",
                    example = "1",
                    required = true
            )
            @PathVariable Long eventId
    );
}