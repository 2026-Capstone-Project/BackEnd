package com.project.backend.domain.nlp.dto.request;

import com.project.backend.domain.nlp.enums.ItemType;
import com.project.backend.domain.nlp.enums.RecurrenceFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class NlpReqDTO {

    public record ParseReq(
            @NotBlank(message = "입력 텍스트는 필수입니다.")
            String text,

            LocalDate baseDate
    ) {}

    public record ConfirmReq(
            @Valid
            @NotEmpty(message = "저장할 항목이 없습니다.")
            List<ConfirmItem> items
    ) {}

    public record ConfirmItem(
            @NotNull(message = "타입은 필수입니다.")
            ItemType type,

            @NotBlank(message = "제목은 필수입니다.")
            String title,

            @NotNull(message = "날짜는 필수입니다.")
            LocalDate date,

            LocalTime time,

            boolean isRecurring,

            RecurrenceRule recurrenceRule
    ) {}

    @Builder
    public record RecurrenceRule(
            RecurrenceFrequency frequency,
            List<String> daysOfWeek,
            Integer interval,
            LocalDate endDate
    ) {
        public int getIntervalOrDefault() {
            return interval != null ? interval:1;
        }
    }
}
