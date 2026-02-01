package com.project.backend.domain.todo.service.command;

import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;

public interface TodoCommandService {

    /**
     * 할 일 생성
     */
    TodoResDTO.TodoInfo createTodo(Long memberId, TodoReqDTO.CreateTodo reqDTO);
}
