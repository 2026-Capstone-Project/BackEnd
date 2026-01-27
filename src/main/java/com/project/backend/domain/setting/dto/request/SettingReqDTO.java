package com.project.backend.domain.setting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SettingReqDTO {

    public record UpdateDailyBriefingTimeReq(
            @NotBlank
            @Pattern(
                    regexp = "^([01]\\d|2[0-3]):00$",
                    message = "HH:MM 형식, 00:00 ~ 23:00 범위의 정시"
            )
            String dailyBriefingTime
    ) {
    }
}
