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

    // ------------------ toggle ------------------
    // 데일리 브리핑 토글
    @Override
    public SettingResDTO.ToggleDailyBriefingRes toggleDailyBriefing(Long memberId) {

        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        setting.toggleDailyBriefing();

        return SettingConverter.toToggleDailyBriefingRes(setting);
    }

    // 선제적 제안 토글
    @Override
    public SettingResDTO.ToggleSuggestionRes toggleSuggestion(Long memberId) {

        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        setting.toggleSuggestion();

        return SettingConverter.toToggleSuggestionRes(setting);
    }

    // ------------------ update ------------------
    // 데일리 브리핑 시간 변경
    @Override
    public SettingResDTO.UpdateDailyBriefingTimeRes updateDailyBriefingTime(
            Long memberId,
            SettingReqDTO.UpdateDailyBriefingTimeReq reqDTO
    ) {

        LocalTime dailyBriefingTime = LocalTime.parse(reqDTO.dailyBriefingTime());

        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        setting.updateDailyBriefingTime(dailyBriefingTime);

        return SettingConverter.toUpdateDailyBriefingTimeRes(setting);
    }

    // 리마인더 시간 변경
    @Override
    public SettingResDTO.UpdateReminderTimingRes updateReminderTiming(
            Long memberId,
            SettingReqDTO.UpdateReminderTimingReq reqDTO
    ) {

        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        setting.updateReminderTiming(reqDTO.reminderTiming());

        return SettingConverter.toUpdateReminderTimingRes(setting);
    }

    // 월간 뷰 종류 변경
    @Override
    public SettingResDTO.UpdateDefaultViewRes updateDefaultView(
            Long memberId,
            SettingReqDTO.UpdateDefaultViewReq reqDTO
    ) {

        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        setting.updateDefaultView(reqDTO.defaultView());

        return SettingConverter.toUpdateDefaultViewRes(setting);
    }
}
