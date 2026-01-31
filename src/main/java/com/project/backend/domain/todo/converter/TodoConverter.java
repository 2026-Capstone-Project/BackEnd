package com.project.backend.domain.todo.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.entity.RecurringTodo;
import com.project.backend.domain.todo.entity.Todo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TodoConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Todo toTodo(TodoReqDTO.CreateTodo reqDTO, Member member, RecurringTodo recurringTodo) {
        LocalDateTime dueDateTime = combineDateAndTime(reqDTO.dueDate(), reqDTO.dueTime());

        return Todo.builder()
                .title(reqDTO.title())
                .dueTime(dueDateTime)
                .isAllDay(reqDTO.isAllDay())
                .priority(reqDTO.priority())
                .memo(reqDTO.memo())
                .member(member)
                .recurringTodo(recurringTodo)
                .build();
    }

    public static RecurringTodo toRecurringTodo(TodoReqDTO.CreateTodo reqDTO, Member member) {
        String customDaysJson = convertCustomDaysToJson(reqDTO.recurrence().customDays());

        return RecurringTodo.builder()
                .title(reqDTO.title())
                .dueTime(reqDTO.dueTime())
                .isAllDay(reqDTO.isAllDay())
                .priority(reqDTO.priority())
                .memo(reqDTO.memo())
                .recurrenceType(reqDTO.recurrence().type())
                .customDays(customDaysJson)
                .startDate(reqDTO.dueDate())
                .endDate(reqDTO.recurrence().endDate())
                .repeatCount(reqDTO.recurrence().repeatCount())
                .member(member)
                .build();
    }

    public static TodoResDTO.TodoInfo toTodoInfo(Todo todo) {
        LocalDate dueDate = null;
        LocalTime dueTime = null;

        if (todo.getDueTime() != null) {
            dueDate = todo.getDueTime().toLocalDate();
            dueTime = todo.getDueTime().toLocalTime();
        }

        return TodoResDTO.TodoInfo.builder()
                .id(todo.getId())
                .title(todo.getTitle())
                .dueDate(dueDate)
                .dueTime(dueTime)
                .isAllDay(todo.getIsAllDay())
                .priority(todo.getPriority())
                .memo(todo.getMemo())
                .isCompleted(todo.getIsCompleted())
                .recurringTodoId(todo.getRecurringTodo() != null ? todo.getRecurringTodo().getId() : null)
                .build();
    }

    private static LocalDateTime combineDateAndTime(LocalDate date, LocalTime time) {
        if (date == null) {
            return null;
        }
        if (time == null) {
            return date.atStartOfDay();
        }
        return LocalDateTime.of(date, time);
    }

    private static String convertCustomDaysToJson(List<String> customDays) {
        if (customDays == null || customDays.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(customDays);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
