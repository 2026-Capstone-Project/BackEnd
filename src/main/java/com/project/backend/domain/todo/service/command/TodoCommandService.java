package com.project.backend.domain.todo.service.command;

import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.enums.RecurrenceUpdateScope;

import java.time.LocalDate;

public interface TodoCommandService {

    /**
     * 할 일 생성
     */
    TodoResDTO.TodoInfo createTodo(Long memberId, TodoReqDTO.CreateTodo reqDTO);

    /**
     * 할 일 수정
     */
    TodoResDTO.TodoInfo updateTodo(Long memberId, Long todoId, LocalDate occurrenceDate,
                                    RecurrenceUpdateScope scope, TodoReqDTO.UpdateTodo reqDTO);

    /**
     * 할 일 삭제
     */
    void deleteTodo(Long memberId, Long todoId, LocalDate occurrenceDate, RecurrenceUpdateScope scope);
}
