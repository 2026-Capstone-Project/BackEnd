package com.project.backend.domain.setting.controller;

import com.project.backend.domain.setting.controller.docs.SettingDocs;
import com.project.backend.domain.setting.dto.request.SettingReqDTO;
import com.project.backend.domain.setting.dto.response.SettingResDTO;
import com.project.backend.domain.setting.service.command.SettingCommandService;
import com.project.backend.domain.setting.service.query.SettingQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingController implements SettingDocs {

    private final SettingCommandService settingCommandService;
    private final SettingQueryService settingQueryService;

    // 전체 설정 조회
    @GetMapping()
    public CustomResponse<SettingResDTO.AllSettingsRes> getSettings(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        SettingResDTO.AllSettingsRes resDTO =
                settingQueryService.getSettings(customUserDetails.getId());
        return CustomResponse.onSuccess("모든 설정 조회 완료", resDTO);
    }

    // ------------------ toggle ------------------
    // 데일리 브리핑 토글
    @PatchMapping("/daily-briefing")
    public CustomResponse<SettingResDTO.ToggleDailyBriefingRes> toggleDailyBriefing(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        SettingResDTO.ToggleDailyBriefingRes resDTO = settingCommandService.toggleDailyBriefing(customUserDetails.getId());
        return CustomResponse.onSuccess("오늘의 브리핑 설정 변경 완료", resDTO);
    }

    // 선제적 제안 토글
    @PatchMapping("/suggestion")
    public CustomResponse<SettingResDTO.ToggleSuggestionRes> toggleSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        SettingResDTO.ToggleSuggestionRes resDTO =
                settingCommandService.toggleSuggestion(customUserDetails.getId());
        return CustomResponse.onSuccess("선제적 제안 설정 변경 완료", resDTO);
    }

    // ------------------ update ------------------
    // 데일리 브리핑 시간 변경
    @PatchMapping("/daily-briefing/time")
    public CustomResponse<SettingResDTO.UpdateDailyBriefingTimeRes> updateDailyBriefingTime(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody SettingReqDTO.UpdateDailyBriefingTimeReq reqDTO
    ) {
        SettingResDTO.UpdateDailyBriefingTimeRes resDTO =
                settingCommandService.updateDailyBriefingTime(customUserDetails.getId(), reqDTO);

        return CustomResponse.onSuccess("오늘의 브리핑 시간 변경 완료", resDTO);
    }

    // 리마인더 시간 변경
    @PatchMapping("/reminder/timing")
    public CustomResponse<SettingResDTO.UpdateReminderTimingRes> updateReminderTiming(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody SettingReqDTO.UpdateReminderTimingReq reqDTO
    ) {
        SettingResDTO.UpdateReminderTimingRes resDTO =
                settingCommandService.updateReminderTiming(customUserDetails.getId(), reqDTO);

        return CustomResponse.onSuccess("리마인더 타이밍 변경 완료", resDTO);
    }

    // 월간 뷰 종류 변경
    @PatchMapping("/default-view")
    public CustomResponse<SettingResDTO.UpdateDefaultViewRes> updateDefaultView(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody SettingReqDTO.UpdateDefaultViewReq reqDTO
    ) {
        SettingResDTO.UpdateDefaultViewRes resDTO =
                settingCommandService.updateDefaultView(customUserDetails.getId(), reqDTO);
        return CustomResponse.onSuccess("기본 뷰 변경 완료", resDTO);
    }
}
