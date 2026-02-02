package com.project.backend.domain.todo.dto.request;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.todo.enums.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TodoReqDTO {

    /**
     * 할 일 생성 요청
     */
    public record CreateTodo(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
            String title,

            @NotNull(message = "시작일은 필수입니다.")
            LocalDate startDate,

            LocalTime dueTime,

            @NotNull(message = "종일 여부는 필수입니다.")
            Boolean isAllDay,

            @NotNull(message = "우선순위는 필수입니다.")
            Priority priority,

            String memo,

            @Valid
            RecurrenceGroupReq recurrenceGroup
    ) {}

    /**
     * 반복 그룹 요청
     */
    public record RecurrenceGroupReq(
            @NotNull(message = "반복 주기는 필수입니다.")
            RecurrenceFrequency frequency,

            Integer intervalValue,

            List<DayOfWeek> daysOfWeek,

            MonthlyType monthlyType,

            List<Integer> daysOfMonth,

            Integer weekOfMonth,

            DayOfWeek dayOfWeekInMonth,

            @NotNull(message = "종료 조건은 필수입니다.")
            RecurrenceEndType endType,

            LocalDate endDate,

            Integer occurrenceCount
    ) {}

    /**
     * 할 일 수정 요청
     */
    public record UpdateTodo(
            @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
            String title,

            LocalDate startDate,

            LocalDate endDate,  // 반복 할 일 종료일 변경

            LocalTime dueTime,

            Boolean isAllDay,

            Priority priority,

            String memo,

            @Valid
            RecurrenceGroupReq recurrenceGroup
    ) {}
}
