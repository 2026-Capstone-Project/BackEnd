package com.project.backend.domain.todo.service.query;

import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.enums.TodoFilter;

import java.time.LocalDate;
import com.project.backend.domain.reminder.dto.NextOccurrenceResult;
import com.project.backend.domain.reminder.entity.Reminder;

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

    /**
     * 반복 할 일의 유효한 날짜인지 검증
     */
    boolean isValidOccurrenceDate(Long todoId, LocalDate occurrenceDate);

    /**
     * 할 일의 반복 여부 판단에 따른 리마인더 occurrenceTime 갱신
     */
    NextOccurrenceResult calculateNextOccurrence(Reminder reminder);
}
