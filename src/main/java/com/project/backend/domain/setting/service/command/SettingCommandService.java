package com.project.backend.domain.setting.service.command;

import com.project.backend.domain.setting.dto.response.SettingResDTO;

public interface SettingCommandService {

    SettingResDTO.DailyBriefingRes toggleDailyBriefing(Long memberId);
}
