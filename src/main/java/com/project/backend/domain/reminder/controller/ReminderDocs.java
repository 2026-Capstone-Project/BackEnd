package com.project.backend.domain.reminder.controller;

import com.project.backend.domain.reminder.dto.response.ReminderResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Tag(name = "리마인더 API", description = "리마인더 API")
public interface ReminderDocs {
    @Operation(
            summary = "리마인더 조회",
            description = """
                    로그인한 사용자의 리마인더 목록을 조회합니다.
                    
                    - 설정한 리마인더 시간 기준으로 현재 시점에 노출 가능한 리마인더만 반환됩니다.
                    - 노출 가능한 리마인더가 없는 경우 빈 리스트를 반환합니다.
                    - 노출 가능한 리마인더가 있는 경우 리마인더 리스트를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "리마인더 조회 성공 (빈 리스트 또는 리마인더 리스트)",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = ReminderResDTO.DetailRes.class)
                            )
                    )
            )
    })
    CustomResponse<List<ReminderResDTO.DetailRes>> getReminders(
            CustomUserDetails customUserDetails
    );
}
