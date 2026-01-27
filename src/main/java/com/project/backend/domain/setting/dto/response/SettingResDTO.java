package com.project.backend.domain.setting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.backend.domain.setting.enums.ReminderTiming;
import lombok.Builder;

import java.time.LocalTime;


public class SettingResDTO {

    @Builder
    public record ToggleDailyBriefingRes(
            boolean dailyBriefingEnabled
    ) {
    }

    @Builder
    public record UpdateDailyBriefingTimeRes(
            @JsonFormat(pattern = "HH:mm")
            LocalTime dailyBriefingTime
    ) {
    }

    @Builder
    public record UpdateReminderTimingRes(
            ReminderTiming reminderTiming
    ) {
    }

    @Builder
    public record ToggleSuggestionRes(
            boolean suggestionEnabled
    ) {
    }
}
