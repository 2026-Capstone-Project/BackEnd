package com.project.backend.domain.todo.dto.response;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.todo.enums.Priority;
import com.project.backend.domain.todo.enums.TodoColor;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TodoResDTO {

    /**
     * 할 일 생성/수정 응답
     */
    @Builder
    public record TodoInfo(
            Long todoId,
            LocalDate occurrenceDate,  // THIS_TODO 수정 시 해당 날짜, 그 외는 null
            String title,
            LocalDate startDate,
            LocalDate endDate,         // 반복 할 일의 종료일 (단일 할 일은 null)
            LocalTime dueTime,
            Boolean isAllDay,
            Priority priority,
            TodoColor color,
            String memo,
            Boolean isCompleted,
            Boolean isRecurring,
            Long recurrenceGroupId
    ) {}

    /**
     * 목록 조회용 아이템
     */
    @Builder
    public record TodoListItem(
            Long todoId,
            LocalDate occurrenceDate,
            String title,
            LocalTime dueTime,
            Boolean isAllDay,
            Priority priority,
            TodoColor color,
            String memo,
            Boolean isCompleted,
            Boolean isRecurring
    ) {}

    /**
     * 목록 조회 응답
     */
    @Builder
    public record TodoListRes(
            List<TodoListItem> todos
    ) {}

    /**
     * 상세 조회 응답
     */
    @Builder
    public record TodoDetailRes(
            Long todoId,
            LocalDate occurrenceDate,
            String title,
            LocalTime dueTime,
            Boolean isAllDay,
            Priority priority,
            TodoColor color,
            String memo,
            Boolean isCompleted,
            Boolean isRecurring,
            RecurrenceGroupRes recurrenceGroup
    ) {}

    /**
     * 반복 그룹 응답
     */
    @Builder
    public record RecurrenceGroupRes(
            RecurrenceFrequency frequency,
            Integer intervalValue,
            List<DayOfWeek> daysOfWeek,
            MonthlyType monthlyType,
            List<Integer> daysOfMonth,
            Integer weekOfMonth,
            DayOfWeek dayOfWeekInMonth,
            RecurrenceEndType endType,
            LocalDate endDate,
            Integer occurrenceCount
    ) {}

    /**
     * 진행 상황 조회 응답
     */
    @Builder
    public record TodoProgressRes(
            LocalDate date,
            Integer totalCount,
            Integer completedCount,
            Double progressRate
    ) {}

    /**
     * 완료/미완료 처리 응답
     */
    @Builder
    public record TodoCompleteRes(
            Long todoId,
            LocalDate occurrenceDate,
            String title,
            Boolean isCompleted
    ) {}
}
