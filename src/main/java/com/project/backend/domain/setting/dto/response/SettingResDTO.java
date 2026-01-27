package com.project.backend.domain.setting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.backend.domain.setting.enums.DefaultView;
import com.project.backend.domain.setting.enums.ReminderTiming;
import lombok.Builder;

import java.time.LocalTime;


public class SettingResDTO {

    @Builder
    public record AllSettingsRes(
            boolean dailyBriefingEnabled,

            @JsonFormat(pattern = "HH:mm")
            LocalTime dailyBriefingTime,

            ReminderTiming reminderTiming,

            boolean suggestionEnabled,

            DefaultView defaultView
    ) {
    }

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

    @Builder
    public record UpdateDefaultViewRes(
            DefaultView defaultView
    ) {
    }
}
