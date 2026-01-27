package com.project.backend.domain.setting.converter;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.setting.dto.response.SettingResDTO;
import com.project.backend.domain.setting.entity.Setting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SettingConverter {

    public static Setting toSetting(Member member){
        return Setting.builder()
                .member(member)
                .build();
    }

    public static SettingResDTO.AllSettingsRes toAllSettingsRes(Setting setting){
        return SettingResDTO.AllSettingsRes.builder()
                .dailyBriefingEnabled(setting.getDailyBriefing())
                .dailyBriefingTime(setting.getDailyBriefingTime())
                .reminderTiming(setting.getReminderTiming())
                .suggestionEnabled(setting.getSuggestion())
                .defaultView(setting.getDefaultView())
                .build();
    }

    public static SettingResDTO.ToggleDailyBriefingRes toToggleDailyBriefingRes(Setting setting){
        return SettingResDTO.ToggleDailyBriefingRes.builder()
                .dailyBriefingEnabled(setting.getDailyBriefing())
                .build();
    }

    public static SettingResDTO.UpdateDailyBriefingTimeRes toUpdateDailyBriefingTimeRes(Setting setting){
        return SettingResDTO.UpdateDailyBriefingTimeRes.builder()
                .dailyBriefingTime(setting.getDailyBriefingTime())
                .build();
    }

    public static SettingResDTO.UpdateReminderTimingRes toUpdateReminderTimingRes(Setting setting){
        return SettingResDTO.UpdateReminderTimingRes.builder()
                .reminderTiming(setting.getReminderTiming())
                .build();
    }

    public static SettingResDTO.ToggleSuggestionRes toToggleSuggestionRes(Setting setting){
        return SettingResDTO.ToggleSuggestionRes.builder()
                .suggestionEnabled(setting.getSuggestion())
                .build();
    }

    public static SettingResDTO.UpdateDefaultViewRes toUpdateDefaultViewRes(Setting setting){
        return SettingResDTO.UpdateDefaultViewRes.builder()
                .defaultView(setting.getDefaultView())
                .build();
    }
}
