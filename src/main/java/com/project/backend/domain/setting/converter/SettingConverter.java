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

    public static SettingResDTO.DailyBriefingRes toDailyBriefingRes(Setting setting){
        return SettingResDTO.DailyBriefingRes.builder()
                .dailyBriefingEnabled(setting.getIsDailyBriefing())
                .build();
    }
}
