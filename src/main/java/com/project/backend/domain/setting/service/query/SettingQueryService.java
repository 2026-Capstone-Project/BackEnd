package com.project.backend.domain.setting.service.query;

import com.project.backend.domain.setting.dto.response.SettingResDTO;

public interface SettingQueryService {

    // 전체 설정 조회
    SettingResDTO.AllSettingsRes getSettings(Long memberId);
}
