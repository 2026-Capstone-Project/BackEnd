package com.project.backend.domain.setting.dto.response;

import lombok.Builder;

public class SettingResDTO {

    @Builder
    public record DailyBriefingRes(
            boolean dailyBriefingEnabled
    ) {
    }
}
