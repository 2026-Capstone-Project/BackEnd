package com.project.backend.domain.setting.service.command;

import com.project.backend.domain.setting.converter.SettingConverter;
import com.project.backend.domain.setting.dto.request.SettingReqDTO;
import com.project.backend.domain.setting.dto.response.SettingResDTO;
import com.project.backend.domain.setting.entity.Setting;
import com.project.backend.domain.setting.exception.SettingErrorCode;
import com.project.backend.domain.setting.exception.SettingException;
import com.project.backend.domain.setting.repository.SettingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettingCommandServiceImpl implements SettingCommandService {

    private final SettingRepository settingRepository;

    @Override
    public SettingResDTO.DailyBriefingRes toggleDailyBriefing(Long memberId) {

        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        setting.toggleDailyBriefing();

        return SettingConverter.toDailyBriefingRes(setting);
    }

    @Override
    public SettingResDTO.DailyBriefingTimeRes updateDailyBriefingTime(
            Long memberId,
            SettingReqDTO.UpdateDailyBriefingTimeReq reqDTO
    ) {

        LocalTime dailyBriefingTime = LocalTime.parse(reqDTO.dailyBriefingTime());

        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        setting.updateDailyBriefingTime(dailyBriefingTime);

        return SettingConverter.toDailyBriefingTimeRes(setting);
    }
}
