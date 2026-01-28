package com.project.backend.domain.setting.service.command;

import com.project.backend.domain.setting.dto.request.SettingReqDTO;
import com.project.backend.domain.setting.dto.response.SettingResDTO;

public interface SettingCommandService {

    // ------------------ toggle ------------------
    // 데일리 브리핑 토글
    SettingResDTO.ToggleDailyBriefingRes toggleDailyBriefing(Long memberId);

    // 선제적 제안 토글
    SettingResDTO.ToggleSuggestionRes toggleSuggestion(Long memberId);

    // ------------------ update ------------------
    // 데일리 브리핑 시간 변경
    SettingResDTO.UpdateDailyBriefingTimeRes updateDailyBriefingTime(
            Long memberId,
            SettingReqDTO.UpdateDailyBriefingTimeReq reqDTO
    );

    // 리마인더 시간 변경
    SettingResDTO.UpdateReminderTimingRes updateReminderTiming(
            Long memberId,
            SettingReqDTO.UpdateReminderTimingReq reqDTO
    );

    // 월간 뷰 종류 변경
    SettingResDTO.UpdateDefaultViewRes updateDefaultView(
            Long memberId,
            SettingReqDTO.UpdateDefaultViewReq reqDTO
    );
}
