package com.project.backend.domain.setting.service.query;

import com.project.backend.domain.setting.dto.response.SettingResDTO;

public interface SettingQueryService {

    SettingResDTO.AllSettingsRes getSettings(Long memberId);
}
