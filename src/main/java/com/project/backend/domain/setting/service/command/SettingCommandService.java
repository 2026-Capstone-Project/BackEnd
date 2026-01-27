package com.project.backend.domain.setting.service.command;

import com.project.backend.domain.setting.dto.request.SettingReqDTO;
import com.project.backend.domain.setting.dto.response.SettingResDTO;

public interface SettingCommandService {

    SettingResDTO.ToggleDailyBriefingRes toggleDailyBriefing(Long memberId);

    SettingResDTO.UpdateDailyBriefingTimeRes updateDailyBriefingTime(
            Long memberId,
            SettingReqDTO.UpdateDailyBriefingTimeReq reqDTO
    );

    SettingResDTO.UpdateReminderTimingRes updateReminderTiming(
            Long memberId,
            SettingReqDTO.UpdateReminderTimingReq reqDTO
    );

    SettingResDTO.ToggleSuggestionRes toggleSuggestion(Long memberId);
}
