package com.project.backend.domain.todo.controller;

import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.service.TodoService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
public class TodoController implements TodoDocs {

    private final TodoService todoService;

    @Override
    @PostMapping
    public CustomResponse<TodoResDTO.TodoInfo> createTodo(
            @RequestBody @Valid TodoReqDTO.CreateTodo reqDTO,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        TodoResDTO.TodoInfo resDTO = todoService.createTodo(reqDTO, customUserDetails.getId());
        return CustomResponse.onSuccess(HttpStatus.CREATED, "할일 등록 완료", resDTO);
    }
}
