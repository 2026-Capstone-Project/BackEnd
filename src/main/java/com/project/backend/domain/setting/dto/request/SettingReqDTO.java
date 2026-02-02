package com.project.backend.domain.setting.dto.request;

import com.project.backend.domain.setting.enums.DefaultView;
import com.project.backend.domain.setting.enums.ReminderTiming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SettingReqDTO {

    // ------------------ update ------------------
    // 데일리 브리핑 시간 변경
    public record UpdateDailyBriefingTimeReq(
            @NotBlank
            @Pattern(
                    regexp = "^([01]\\d|2[0-3]):00$",
                    message = "HH:00 형식, 00:00 ~ 23:00 범위의 정시"
            )
            String dailyBriefingTime
    ) {
    }

    // 리마인더 시간 변경
    public record UpdateReminderTimingReq(
            ReminderTiming reminderTiming
    ) {
    }

    // 월간 뷰 종류 변경
    public record UpdateDefaultViewReq(
            DefaultView defaultView
    ) {
    }
}
