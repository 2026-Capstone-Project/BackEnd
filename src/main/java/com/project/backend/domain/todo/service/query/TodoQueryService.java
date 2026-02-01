package com.project.backend.domain.todo.service.query;

import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.enums.TodoFilter;

import java.time.LocalDate;

public interface TodoQueryService {

    /**
     * 할 일 목록 조회
     * - 단일 할 일: 전부 조회
     * - 반복 할 일: 다음 1개만 표시
     */
    TodoResDTO.TodoListRes getTodos(Long memberId, TodoFilter filter);

    /**
     * 캘린더용 할 일 조회
     * - 범위 내 할 일 모두 조회
     * - 반복 할 일: 범위 내 모두 펼침
     */
    TodoResDTO.TodoListRes getTodosForCalendar(Long memberId, LocalDate startDate, LocalDate endDate);

    /**
     * 할 일 상세 조회
     */
    TodoResDTO.TodoDetailRes getTodoDetail(Long memberId, Long todoId, LocalDate occurrenceDate);

    /**
     * 진행 상황 조회
     */
    TodoResDTO.TodoProgressRes getProgress(Long memberId, LocalDate date);
}
