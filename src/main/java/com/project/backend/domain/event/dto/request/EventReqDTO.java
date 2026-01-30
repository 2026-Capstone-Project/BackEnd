package com.project.backend.domain.event.dto.request;

import com.project.backend.domain.event.enums.EventColor;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EventReqDTO {

    public record CreateReq(
            @NotBlank(message = "제목은 필수입니다.")
            String title,
            String content,
            @NotNull(message = "날짜 입력은 필수입니다.")
            LocalDateTime startTime,
            @NotNull(message = "날짜 입력은 필수입니다.")
            LocalDateTime endTime,
            String location,
            EventColor color,
            Boolean isAllDay,

            @Schema(
                    description = "반복 일정 설정 (반복 미사용 시 아예 보내지 않음)",
                    implementation = RecurrenceGroupReqDTO.CreateReq.class,
                    nullable = true
            )
            @Valid
            RecurrenceGroupReqDTO.CreateReq recurrenceGroup
    ) {
    }

    @Builder
    public record UpdateReq(
            LocalDate occurrenceDate, // 계산된 날짜
            String title,
            String content,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location,
            EventColor color,
            Boolean isAllDay,
            RecurrenceUpdateScope recurrenceUpdateScope,
            RecurrenceGroupReqDTO.UpdateReq recurrenceGroup
    ) {
    }
}
