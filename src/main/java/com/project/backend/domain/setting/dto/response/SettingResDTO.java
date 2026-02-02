package com.project.backend.domain.setting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.backend.domain.setting.enums.DefaultView;
import com.project.backend.domain.setting.enums.ReminderTiming;
import lombok.Builder;

import java.time.LocalTime;


public class SettingResDTO {

    // 전체 설정 조회
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

    // ------------------ toggle ------------------
    // 데일리 브리핑 토글
    @Builder
    public record ToggleDailyBriefingRes(
            boolean dailyBriefingEnabled
    ) {
    }

    // 선제적 제안 토글
    @Builder
    public record ToggleSuggestionRes(
            boolean suggestionEnabled
    ) {
    }

    // ------------------ update ------------------
    // 데일리 브리핑 시간 변경
    @Builder
    public record UpdateDailyBriefingTimeRes(
            @JsonFormat(pattern = "HH:mm")
            LocalTime dailyBriefingTime
    ) {
    }

    // 리마인더 시간 변경
    @Builder
    public record UpdateReminderTimingRes(
            ReminderTiming reminderTiming
    ) {
    }

    // 월간 뷰 종류 변경
    @Builder
    public record UpdateDefaultViewRes(
            DefaultView defaultView
    ) {
    }
}
