package com.project.backend.domain.setting.service.command;

import com.project.backend.domain.setting.dto.request.SettingReqDTO;
import com.project.backend.domain.setting.dto.response.SettingResDTO;

public interface SettingCommandService {

    SettingResDTO.DailyBriefingRes toggleDailyBriefing(Long memberId);

    SettingResDTO.DailyBriefingTimeRes updateDailyBriefingTime(
            Long memberId,
            SettingReqDTO.UpdateDailyBriefingTimeReq reqDTO
    );

    SettingResDTO.UpdateReminderTimingRes updateReminderTiming(
            Long memberId,
            SettingReqDTO.UpdateReminderTimingReq reqDTO
    );
}
