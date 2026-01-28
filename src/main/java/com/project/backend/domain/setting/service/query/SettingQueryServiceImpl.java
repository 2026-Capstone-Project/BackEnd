package com.project.backend.domain.setting.service.query;

import com.project.backend.domain.setting.converter.SettingConverter;
import com.project.backend.domain.setting.dto.response.SettingResDTO;
import com.project.backend.domain.setting.entity.Setting;
import com.project.backend.domain.setting.exception.SettingErrorCode;
import com.project.backend.domain.setting.exception.SettingException;
import com.project.backend.domain.setting.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingQueryServiceImpl implements SettingQueryService {

    private final SettingRepository settingRepository;

    // 전체 설정 조회
    @Override
    public SettingResDTO.AllSettingsRes getSettings(Long memberId) {

        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        return SettingConverter.toAllSettingsRes(setting);
    }
}
