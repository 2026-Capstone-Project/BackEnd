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

@Tag(name = "자연어 처리 API", description = "자연어 일정/할 일 처리 API")
public interface NlpDocs {

    @Operation(
            summary = "자연어 파싱",
            description = """
                자연어 입력을 분석하여 일정(Event) 또는 할 일(Todo) 정보를 추출합니다.
                
                **지원 기능:**
                - 단일/복수 항목 파싱
                - 반복 일정 감지 (매일, 매주, 매월, 매년)
                - 상대적 날짜 변환 (내일, 다음 주 등)
                - 모호한 입력 감지 및 선택지 제공
                
                **입력 예시:**
                - "내일 3시 팀 미팅"
                - "금요일까지 보고서 제출"
                - "매주 월수금 헬스장"
                - "내일 치과, 금요일까지 과제 제출"
                """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "자연어 파싱 요청",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = NlpReqDTO.ParseReq.class),
                    examples = {
                            @ExampleObject(
                                    name = "단일 일정",
                                    value = """
                                        {
                                            "text": "내일 오후 3시 팀 미팅",
                                            "baseDate": "2026-01-20"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "반복 일정",
                                    value = """
                                        {
                                            "text": "매주 월수금 저녁 7시 헬스장",
                                            "baseDate": "2026-01-20"
                                        }
                                        """
                            ),
                            @ExampleObject(
                                    name = "복수 항목",
                                    value = """
                                        {
                                            "text": "내일 치과 예약, 금요일까지 보고서 제출",
                                            "baseDate": "2026-01-20"
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
                    content = @Content(schema = @Schema(implementation = NlpResDTO.ParseRes.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (입력 텍스트 누락 등)"
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "파싱 실패 (LLM이 입력을 이해하지 못함)"
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "AI 서비스 일시적 오류"
            )
    })
    CustomResponse<NlpResDTO.ParseRes> parse(
            @Valid @RequestBody NlpReqDTO.ParseReq reqDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

//    @Operation(
//            summary = "파싱 결과 확정 및 저장",
//            description = """
//                사용자가 확인/수정한 파싱 결과를 일정(Event) 또는 할 일(Todo)로 저장합니다.
//
//                **처리 흐름:**
//                1. 파싱 결과 검증
//                2. 반복 일정인 경우 RecurrenceGroup 생성
//                3. Event 또는 Todo 엔티티 저장
//                4. 저장 결과 반환
//
//                **반복 설정 옵션:**
//                - frequency: DAILY, WEEKLY, MONTHLY, YEARLY
//                - interval: 반복 간격 (매 N일/주/월/년)
//                - daysOfWeek: 요일 선택 (WEEKLY일 때)
//                - monthlyType: DAY_OF_MONTH(매월 N일) / DAY_OF_WEEK(매월 N번째 X요일)
//                - endType: NEVER / END_BY_DATE / END_BY_COUNT
//                """
//    )
//    @io.swagger.v3.oas.annotations.parameters.RequestBody(
//            description = "파싱 결과 확정 요청",
//            required = true,
//            content = @Content(
//                    schema = @Schema(implementation = NlpReqDTO.ConfirmReq.class),
//                    examples = {
//                            @ExampleObject(
//                                    name = "단일 일정 저장",
//                                    value = """
//                                        {
//                                            "items": [
//                                                {
//                                                    "itemId": "550e8400-e29b-41d4-a716-446655440000",
//                                                    "type": "EVENT",
//                                                    "title": "팀 미팅",
//                                                    "date": "2026-01-21",
//                                                    "startTime": "15:00",
//                                                    "endTime": "16:00",
//                                                    "isAllDay": false,
//                                                    "isRecurring": false
//                                                }
//                                            ]
//                                        }
//                                        """
//                            ),
//                            @ExampleObject(
//                                    name = "반복 일정 저장",
//                                    value = """
//                                        {
//                                            "items": [
//                                                {
//                                                    "itemId": "550e8400-e29b-41d4-a716-446655440001",
//                                                    "type": "EVENT",
//                                                    "title": "헬스장",
//                                                    "date": "2026-01-20",
//                                                    "startTime": "19:00",
//                                                    "endTime": "20:30",
//                                                    "isAllDay": false,
//                                                    "isRecurring": true,
//                                                    "recurrenceRule": {
//                                                        "frequency": "WEEKLY",
//                                                        "interval": 1,
//                                                        "daysOfWeek": ["MON", "WED", "FRI"],
//                                                        "endType": "END_BY_DATE",
//                                                        "endDate": "2026-04-20"
//                                                    }
//                                                }
//                                            ]
//                                        }
//                                        """
//                            ),
//                            @ExampleObject(
//                                    name = "할 일 저장",
//                                    value = """
//                                        {
//                                            "items": [
//                                                {
//                                                    "itemId": "550e8400-e29b-41d4-a716-446655440002",
//                                                    "type": "TODO",
//                                                    "title": "보고서 제출",
//                                                    "date": "2026-01-24",
//                                                    "startTime": "23:59",
//                                                    "isAllDay": false,
//                                                    "isRecurring": false
//                                                }
//                                            ]
//                                        }
//                                        """
//                            )
//                    }
//            )
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "저장 성공",
//                    content = @Content(schema = @Schema(implementation = NlpResDTO.ConfirmRes.class))
//            ),
//            @ApiResponse(
//                    responseCode = "400",
//                    description = "잘못된 요청 (필수 필드 누락, 잘못된 타입 등)"
//            ),
//            @ApiResponse(
//                    responseCode = "404",
//                    description = "회원을 찾을 수 없음"
//            )
//    })
//    CustomResponse<NlpResDTO.ConfirmRes> confirm(
//            @Valid @RequestBody NlpReqDTO.ConfirmReq reqDTO,
//            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
//    );
}
