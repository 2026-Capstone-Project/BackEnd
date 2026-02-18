package com.project.backend.domain.todo.converter;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceException;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import com.project.backend.domain.todo.enums.Priority;
import com.project.backend.domain.todo.enums.TodoColor;
import com.project.backend.global.recurrence.util.RecurrenceUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TodoConverter {

    // ===== Entity 변환 =====

    /**
     * CreateTodo 요청 → Todo 엔티티
     */
    public static Todo toTodo(TodoReqDTO.CreateTodo reqDTO, Member member, TodoRecurrenceGroup todoRecurrenceGroup) {
        return Todo.builder()
                .title(reqDTO.title())
                .startDate(reqDTO.startDate())
                .dueTime(reqDTO.dueTime())
                .isAllDay(reqDTO.isAllDay() != null ? reqDTO.isAllDay() : false)
                .priority(reqDTO.priority() != null ? reqDTO.priority() : Priority.MEDIUM)
                .color(reqDTO.color() != null ? reqDTO.color() : TodoColor.BLUE)
                .memo(reqDTO.memo())
                .member(member)
                .todoRecurrenceGroup(todoRecurrenceGroup)
                .build();
    }

    /**
     * RecurrenceGroupReq 요청 → TodoRecurrenceGroup 엔티티
     */
    public static TodoRecurrenceGroup toTodoRecurrenceGroup(TodoReqDTO.RecurrenceGroupReq reqDTO, Member member) {
        if (reqDTO == null) {
            return null;
        }

        // List<DayOfWeek> → "MONDAY,WEDNESDAY,FRIDAY" 형태로 변환
        String daysOfWeek = reqDTO.daysOfWeek() != null
                ? reqDTO.daysOfWeek().stream()
                        .map(DayOfWeek::name)
                        .collect(Collectors.joining(","))
                : null;

        // List<Integer> → "1,15" 형태로 변환
        String daysOfMonth = reqDTO.daysOfMonth() != null
                ? reqDTO.daysOfMonth().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))
                : null;

        // DayOfWeek → "MONDAY" 형태로 변환
        String dayOfWeekInMonth = reqDTO.dayOfWeekInMonth() != null
                ? reqDTO.dayOfWeekInMonth().name()
                : null;

        return TodoRecurrenceGroup.create(
                member,
                reqDTO.frequency(),
                reqDTO.intervalValue() != null ? reqDTO.intervalValue() : 1,
                daysOfWeek,
                reqDTO.monthlyType(),
                daysOfMonth,
                reqDTO.weekOfMonth(),
                dayOfWeekInMonth,
                reqDTO.endType(),
                reqDTO.endDate(),
                reqDTO.occurrenceCount()
        );
    }

    // ===== Response DTO 변환 =====

    /**
     * Todo → TodoInfo (생성/수정 응답)
     */
    public static TodoResDTO.TodoInfo toTodoInfo(Todo todo) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();
        return TodoResDTO.TodoInfo.builder()
                .todoId(todo.getId())
                .occurrenceDate(null)
                .title(todo.getTitle())
                .startDate(todo.getStartDate())
                .endDate(group != null ? group.getEndDate() : null)
                .dueTime(todo.getDueTime())
                .isAllDay(todo.getIsAllDay())
                .priority(todo.getPriority())
                .color(todo.getColor())
                .memo(todo.getMemo())
                .isCompleted(todo.getIsCompleted())
                .isRecurring(todo.isRecurring())
                .recurrenceGroupId(group != null ? group.getId() : null)
                .build();
    }

    /**
     * Todo + Exception → TodoInfo (THIS_TODO 수정 응답)
     * 예외로 수정된 특정 날짜의 정보를 반환
     */
    public static TodoResDTO.TodoInfo toTodoInfo(Todo todo, LocalDate occurrenceDate,
                                                   TodoRecurrenceException exception) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();
        return TodoResDTO.TodoInfo.builder()
                .todoId(todo.getId())
                .occurrenceDate(occurrenceDate)
                .title(exception.getTitle() != null ? exception.getTitle() : todo.getTitle())
                .startDate(todo.getStartDate())
                .endDate(group != null ? group.getEndDate() : null)
                .dueTime(exception.getDueTime() != null ? exception.getDueTime() : todo.getDueTime())
                .isAllDay(todo.getIsAllDay())
                .priority(exception.getPriority() != null ? exception.getPriority() : todo.getPriority())
                .color(exception.getColor() != null ? exception.getColor() : todo.getColor())
                .memo(exception.getMemo() != null ? exception.getMemo() : todo.getMemo())
                .isCompleted(exception.getIsCompleted())
                .isRecurring(true)
                .recurrenceGroupId(group.getId())
                .build();
    }

    /**
     * Todo → TodoListItem (목록 조회용)
     * 단발성 할 일 또는 반복 할 일의 기본 정보
     */
    public static TodoResDTO.TodoListItem toTodoListItem(Todo todo, LocalDate occurrenceDate, Boolean isCompleted) {
        return TodoResDTO.TodoListItem.builder()
                .todoId(todo.getId())
                .occurrenceDate(occurrenceDate)
                .title(todo.getTitle())
                .dueTime(todo.getDueTime())
                .isAllDay(todo.getIsAllDay())
                .priority(todo.getPriority())
                .color(todo.getColor())
                .memo(todo.getMemo())
                .isCompleted(isCompleted != null ? isCompleted : todo.getIsCompleted())
                .isRecurring(todo.isRecurring())
                .build();
    }

    /**
     * Todo + Exception → TodoListItem (반복 예외가 있는 경우)
     */
    public static TodoResDTO.TodoListItem toTodoListItem(Todo todo, LocalDate occurrenceDate,
                                                          TodoRecurrenceException exception) {
        if (exception == null) {
            return toTodoListItem(todo, occurrenceDate, todo.getIsCompleted());
        }

        return TodoResDTO.TodoListItem.builder()
                .todoId(todo.getId())
                .occurrenceDate(occurrenceDate)
                .title(exception.getTitle() != null ? exception.getTitle() : todo.getTitle())
                .dueTime(exception.getDueTime() != null ? exception.getDueTime() : todo.getDueTime())
                .isAllDay(todo.getIsAllDay())
                .priority(exception.getPriority() != null ? exception.getPriority() : todo.getPriority())
                .color(exception.getColor() != null ? exception.getColor() : todo.getColor())
                .memo(exception.getMemo() != null ? exception.getMemo() : todo.getMemo())
                .isCompleted(exception.getIsCompleted())
                .isRecurring(true)
                .build();
    }

    /**
     * Todo → TodoDetailRes (상세 조회)
     */
    public static TodoResDTO.TodoDetailRes toTodoDetailRes(Todo todo, LocalDate occurrenceDate) {
        return TodoResDTO.TodoDetailRes.builder()
                .todoId(todo.getId())
                .occurrenceDate(occurrenceDate)
                .title(todo.getTitle())
                .dueTime(todo.getDueTime())
                .isAllDay(todo.getIsAllDay())
                .priority(todo.getPriority())
                .color(todo.getColor())
                .memo(todo.getMemo())
                .isCompleted(todo.getIsCompleted())
                .isRecurring(todo.isRecurring())
                .recurrenceGroup(todo.getTodoRecurrenceGroup() != null
                        ? toRecurrenceGroupRes(todo.getTodoRecurrenceGroup())
                        : null)
                .build();
    }

    /**
     * Todo + Exception → TodoDetailRes (반복 예외가 있는 경우)
     */
    public static TodoResDTO.TodoDetailRes toTodoDetailRes(Todo todo, LocalDate occurrenceDate,
                                                            TodoRecurrenceException exception) {
        if (exception == null) {
            return toTodoDetailRes(todo, occurrenceDate);
        }

        return TodoResDTO.TodoDetailRes.builder()
                .todoId(todo.getId())
                .occurrenceDate(occurrenceDate)
                .title(exception.getTitle() != null ? exception.getTitle() : todo.getTitle())
                .dueTime(exception.getDueTime() != null ? exception.getDueTime() : todo.getDueTime())
                .isAllDay(todo.getIsAllDay())
                .priority(exception.getPriority() != null ? exception.getPriority() : todo.getPriority())
                .color(exception.getColor() != null ? exception.getColor() : todo.getColor())
                .memo(exception.getMemo() != null ? exception.getMemo() : todo.getMemo())
                .isCompleted(exception.getIsCompleted())
                .isRecurring(true)
                .recurrenceGroup(toRecurrenceGroupRes(todo.getTodoRecurrenceGroup()))
                .build();
    }

    /**
     * TodoRecurrenceGroup → RecurrenceGroupRes
     */
    public static TodoResDTO.RecurrenceGroupRes toRecurrenceGroupRes(TodoRecurrenceGroup group) {
        if (group == null) {
            return null;
        }

        // "MONDAY,WEDNESDAY" → List<DayOfWeek>
        List<DayOfWeek> daysOfWeek = group.getDaysOfWeek() != null
                ? RecurrenceUtils.parseDaysOfWeek(group.getDaysOfWeek())
                : null;

        // "1,15" → List<Integer>
        List<Integer> daysOfMonth = group.getDaysOfMonth() != null
                ? RecurrenceUtils.parseDaysOfMonth(group.getDaysOfMonth())
                : null;

        // "MONDAY" → DayOfWeek
        DayOfWeek dayOfWeekInMonth = group.getDayOfWeekInMonth() != null
                ? DayOfWeek.valueOf(group.getDayOfWeekInMonth())
                : null;

        return TodoResDTO.RecurrenceGroupRes.builder()
                .frequency(group.getFrequency())
                .intervalValue(group.getIntervalValue())
                .daysOfWeek(daysOfWeek)
                .monthlyType(group.getMonthlyType())
                .daysOfMonth(daysOfMonth)
                .weekOfMonth(group.getWeekOfMonth())
                .dayOfWeekInMonth(dayOfWeekInMonth)
                .endType(group.getEndType())
                .endDate(group.getEndDate())
                .occurrenceCount(group.getOccurrenceCount())
                .build();
    }

    /**
     * 진행 상황 → TodoProgressRes
     */
    public static TodoResDTO.TodoProgressRes toTodoProgressRes(LocalDate date, int totalCount, int completedCount) {
        double progressRate = totalCount > 0
                ? Math.round((double) completedCount / totalCount * 100.0) / 100.0
                : 0.0;

        return TodoResDTO.TodoProgressRes.builder()
                .date(date)
                .totalCount(totalCount)
                .completedCount(completedCount)
                .progressRate(progressRate)
                .build();
    }

    /**
     * 완료/미완료 처리 → TodoCompleteRes
     */
    public static TodoResDTO.TodoCompleteRes toTodoCompleteRes(Todo todo, LocalDate occurrenceDate, boolean isCompleted) {
        return TodoResDTO.TodoCompleteRes.builder()
                .todoId(todo.getId())
                .occurrenceDate(occurrenceDate)
                .title(todo.getTitle())
                .isCompleted(isCompleted)
                .build();
    }
}
