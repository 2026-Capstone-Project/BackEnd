package com.project.backend.domain.todo.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TodoConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Todo toTodo(TodoReqDTO.CreateTodo reqDTO, Member member, TodoRecurrenceGroup todoRecurrenceGroup) {
        return Todo.builder()
                .title(reqDTO.title())
                .dueDate(reqDTO.dueDate())
                .dueTime(reqDTO.dueTime())
                .isAllDay(reqDTO.isAllDay())
                .priority(reqDTO.priority())
                .memo(reqDTO.memo())
                .member(member)
                .todoRecurrenceGroup(todoRecurrenceGroup)
                .build();
    }

    public static TodoResDTO.TodoInfo toTodoInfo(Todo todo) {
        return TodoResDTO.TodoInfo.builder()
                .todoId(todo.getId())
                .title(todo.getTitle())
                .dueDate(todo.getDueDate())
                .dueTime(todo.getDueTime())
                .isAllDay(todo.getIsAllDay())
                .priority(todo.getPriority())
                .memo(todo.getMemo())
                .isCompleted(todo.getIsCompleted())
                .isRecurring(todo.isRecurring())
                .recurrenceGroupId(todo.getTodoRecurrenceGroup() != null ? todo.getTodoRecurrenceGroup().getId() : null)
                .build();
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
