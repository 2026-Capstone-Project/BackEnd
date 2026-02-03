package com.project.backend.domain.todo.controller;

import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.enums.RecurrenceUpdateScope;
import com.project.backend.domain.todo.enums.TodoFilter;
import com.project.backend.domain.todo.service.command.TodoCommandService;
import com.project.backend.domain.todo.service.query.TodoQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
public class TodoController implements TodoDocs {

    private final TodoCommandService todoCommandService;
    private final TodoQueryService todoQueryService;

    /**
     * 할 일 생성
     */
    @Override
    @PostMapping
    public CustomResponse<TodoResDTO.TodoInfo> createTodo(
            @RequestBody @Valid TodoReqDTO.CreateTodo reqDTO,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        TodoResDTO.TodoInfo resDTO = todoCommandService.createTodo(customUserDetails.getId(), reqDTO);
        return CustomResponse.onSuccess(HttpStatus.CREATED, "할 일 등록 완료", resDTO);
    }

    /**
     * 할 일 목록 조회
     */
    @Override
    @GetMapping
    public CustomResponse<TodoResDTO.TodoListRes> getTodos(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(defaultValue = "ALL") TodoFilter filter) {
        TodoResDTO.TodoListRes resDTO = todoQueryService.getTodos(customUserDetails.getId(), filter);
        return CustomResponse.onSuccess("할 일 목록 조회 완료", resDTO);
    }

    /**
     * 캘린더용 할 일 조회
     */
    @Override
    @GetMapping("/calendar")
    public CustomResponse<TodoResDTO.TodoListRes> getTodosForCalendar(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        TodoResDTO.TodoListRes resDTO = todoQueryService.getTodosForCalendar(
                customUserDetails.getId(), startDate, endDate);
        return CustomResponse.onSuccess("캘린더 할 일 조회 완료", resDTO);
    }

    /**
     * 할 일 상세 조회
     */
    @Override
    @GetMapping("/{todoId}")
    public CustomResponse<TodoResDTO.TodoDetailRes> getTodoDetail(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long todoId,
            @RequestParam(required = false) LocalDate occurrenceDate) {
        TodoResDTO.TodoDetailRes resDTO = todoQueryService.getTodoDetail(
                customUserDetails.getId(), todoId, occurrenceDate);
        return CustomResponse.onSuccess("할 일 상세 조회 완료", resDTO);
    }

    /**
     * 진행 상황 조회
     */
    @Override
    @GetMapping("/progress")
    public CustomResponse<TodoResDTO.TodoProgressRes> getProgress(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam LocalDate date) {
        TodoResDTO.TodoProgressRes resDTO = todoQueryService.getProgress(customUserDetails.getId(), date);
        return CustomResponse.onSuccess("진행 상황 조회 완료", resDTO);
    }

    /**
     * 할 일 수정
     */
    @Override
    @PatchMapping("/{todoId}")
    public CustomResponse<TodoResDTO.TodoInfo> updateTodo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long todoId,
            @RequestParam(required = false) LocalDate occurrenceDate,
            @RequestParam(required = false) RecurrenceUpdateScope scope,
            @RequestBody @Valid TodoReqDTO.UpdateTodo reqDTO) {
        TodoResDTO.TodoInfo resDTO = todoCommandService.updateTodo(
                customUserDetails.getId(), todoId, occurrenceDate, scope, reqDTO);
        return CustomResponse.onSuccess("할 일 수정 완료", resDTO);
    }

    /**
     * 할 일 완료 상태 변경
     */
    @Override
    @PatchMapping("/{todoId}/complete")
    public CustomResponse<TodoResDTO.TodoCompleteRes> updateCompleteStatus(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long todoId,
            @RequestParam(required = false) LocalDate occurrenceDate,
            @RequestParam boolean isCompleted) {
        TodoResDTO.TodoCompleteRes resDTO = todoCommandService.updateCompleteStatus(
                customUserDetails.getId(), todoId, occurrenceDate, isCompleted);
        return CustomResponse.onSuccess(isCompleted ? "할 일 완료 처리" : "할 일 미완료 처리", resDTO);
    }

    /**
     * 할 일 삭제
     */
    @Override
    @DeleteMapping("/{todoId}")
    public CustomResponse<Void> deleteTodo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long todoId,
            @RequestParam(required = false) LocalDate occurrenceDate,
            @RequestParam(required = false) RecurrenceUpdateScope scope) {
        todoCommandService.deleteTodo(customUserDetails.getId(), todoId, occurrenceDate, scope);
        return CustomResponse.onSuccess("할 일 삭제 완료", null);
    }
}
