package com.project.backend.domain.setting.controller.docs;

import com.project.backend.domain.setting.dto.request.SettingReqDTO;
import com.project.backend.domain.setting.dto.response.SettingResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Setting", description = "사용자 설정 관리 API")
public interface SettingDocs {

    // ================== 전체 설정 조회 ==================
    @Operation(
            summary = "전체 설정 조회",
            description = "로그인한 사용자의 모든 설정 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "COMMON401",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON401",
                                      "message": "인증이 필요합니다",
                                      "result": null
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "설정 정보 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "SET404",
                                    value = """
                                    {
                                      "isSuccess": false,
                                      "code": "SET404",
                                      "message": "Setting이 존재하지 않습니다",
                                      "result": null
                                    }
                                    """
                            )
                    )
            )
    })
    CustomResponse<SettingResDTO.AllSettingsRes> getSettings(
            @Parameter(hidden = true) CustomUserDetails customUserDetails
    );

    // ================== 데일리 브리핑 토글 ==================
    @Operation(
            summary = "데일리 브리핑 ON/OFF",
            description = "데일리 브리핑 수신 여부를 토글합니다. (요청 Body 없음)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "데일리 브리핑 설정 변경 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "COMMON401", value = """
                            {
                              "isSuccess": false,
                              "code": "COMMON401",
                              "message": "인증이 필요합니다",
                              "result": null
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "설정 정보 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "SET404", value = """
                            {
                              "isSuccess": false,
                              "code": "SET404",
                              "message": "Setting이 존재하지 않습니다",
                              "result": null
                            }
                            """)
                    )
            )
    })
    CustomResponse<SettingResDTO.ToggleDailyBriefingRes> toggleDailyBriefing(
            @Parameter(hidden = true) CustomUserDetails customUserDetails
    );

    // ================== 선제적 제안 토글 ==================
    @Operation(
            summary = "선제적 제안 ON/OFF",
            description = "선제적 제안 기능을 토글합니다. (요청 Body 없음)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선제적 제안 설정 변경 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "COMMON401", value = """
                            {
                              "isSuccess": false,
                              "code": "COMMON401",
                              "message": "인증이 필요합니다",
                              "result": null
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "설정 정보 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "SET404", value = """
                            {
                              "isSuccess": false,
                              "code": "SET404",
                              "message": "Setting이 존재하지 않습니다",
                              "result": null
                            }
                            """)
                    )
            )
    })
    CustomResponse<SettingResDTO.ToggleSuggestionRes> toggleSuggestion(
            @Parameter(hidden = true) CustomUserDetails customUserDetails
    );

    // ================== 데일리 브리핑 시간 변경 ==================
    @Operation(
            summary = "데일리 브리핑 시간 변경",
            description = """
                데일리 브리핑 발송 시간을 변경합니다.

                - 형식: HH:00
                - 범위: 00:00 ~ 23:00
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "브리핑 시간 변경 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "VALID400_1 (DTO_VALIDATION_FAILED)",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "VALID400_1",
                                              "message": "잘못된 DTO 필드입니다.",
                                              "result": {
                                                "dailyBriefingTime": "HH:00 형식, 00:00 ~ 23:00 범위의 정시"
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "VALID400_3 (BAD_REQUEST_BODY)",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "VALID400_3",
                                              "message": "요청 본문을 읽을 수 없음, JSON 문법 오류일 가능성",
                                              "result": null
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "COMMON401", value = """
                            {
                              "isSuccess": false,
                              "code": "COMMON401",
                              "message": "인증이 필요합니다",
                              "result": null
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "설정 정보 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "SET404", value = """
                            {
                              "isSuccess": false,
                              "code": "SET404",
                              "message": "Setting이 존재하지 않습니다",
                              "result": null
                            }
                            """)
                    )
            )
    })
    CustomResponse<SettingResDTO.UpdateDailyBriefingTimeRes> updateDailyBriefingTime(
            @Parameter(hidden = true) CustomUserDetails customUserDetails,
            @Valid @RequestBody SettingReqDTO.UpdateDailyBriefingTimeReq reqDTO
    );

    // ================== 리마인더 타이밍 변경 ==================
    @Operation(
            summary = "리마인더 타이밍 변경",
            description = """
                리마인더 알림 타이밍을 변경합니다.

                지원 값:
                - FIVE_MINUTES / FIFTEEN_MINUTES / THIRTY_MINUTES
                - ONE_HOUR / TWO_HOURS / ONE_DAY
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리마인더 타이밍 변경 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "VALID400_4 (INVALID_ENUM)",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "VALID400_4",
                                              "message": "ENUM 입력 오류",
                                              "result": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "VALID400_3 (BAD_REQUEST_BODY)",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "VALID400_3",
                                              "message": "요청 본문을 읽을 수 없음, JSON 문법 오류일 가능성",
                                              "result": null
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "COMMON401", value = """
                            {
                              "isSuccess": false,
                              "code": "COMMON401",
                              "message": "인증이 필요합니다",
                              "result": null
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "설정 정보 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "SET404", value = """
                            {
                              "isSuccess": false,
                              "code": "SET404",
                              "message": "Setting이 존재하지 않습니다",
                              "result": null
                            }
                            """)
                    )
            )
    })
    CustomResponse<SettingResDTO.UpdateReminderTimingRes> updateReminderTiming(
            @Parameter(hidden = true) CustomUserDetails customUserDetails,
            @RequestBody SettingReqDTO.UpdateReminderTimingReq reqDTO
    );

    // ================== 기본 뷰 변경 ==================
    @Operation(
            summary = "기본 캘린더 뷰 변경",
            description = """
                캘린더 기본 뷰를 변경합니다.

                지원 값:
                - MONTH / WEEK / DAY
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "기본 뷰 변경 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "VALID400_4 (INVALID_ENUM)",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "VALID400_4",
                                              "message": "ENUM 입력 오류",
                                              "result": null
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "VALID400_3 (BAD_REQUEST_BODY)",
                                            value = """
                                            {
                                              "isSuccess": false,
                                              "code": "VALID400_3",
                                              "message": "요청 본문을 읽을 수 없음, JSON 문법 오류일 가능성",
                                              "result": null
                                            }
                                            """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "COMMON401", value = """
                            {
                              "isSuccess": false,
                              "code": "COMMON401",
                              "message": "인증이 필요합니다",
                              "result": null
                            }
                            """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "설정 정보 없음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "SET404", value = """
                            {
                              "isSuccess": false,
                              "code": "SET404",
                              "message": "Setting이 존재하지 않습니다",
                              "result": null
                            }
                            """)
                    )
            )
    })
    CustomResponse<SettingResDTO.UpdateDefaultViewRes> updateDefaultView(
            @Parameter(hidden = true) CustomUserDetails customUserDetails,
            @RequestBody SettingReqDTO.UpdateDefaultViewReq reqDTO
    );
}
