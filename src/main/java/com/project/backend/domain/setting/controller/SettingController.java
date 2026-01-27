package com.project.backend.domain.setting.controller;

import com.project.backend.domain.setting.controller.docs.SettingDocs;
import com.project.backend.domain.setting.dto.response.SettingResDTO;
import com.project.backend.domain.setting.service.command.SettingCommandService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingController implements SettingDocs {

    private final SettingCommandService settingCommandService;

    @PostMapping("/daily-briefing")
    public CustomResponse<SettingResDTO.DailyBriefingRes> toggleDailyBriefing(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        SettingResDTO.DailyBriefingRes resDTO = settingCommandService.toggleDailyBriefing(customUserDetails.getId());
        return CustomResponse.onSuccess("오늘의 브리핑 설정 변경 완료", resDTO);
    }
}
