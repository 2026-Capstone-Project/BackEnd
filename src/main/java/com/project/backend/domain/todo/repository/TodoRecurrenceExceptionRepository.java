package com.project.backend.domain.todo.repository;

import com.project.backend.domain.todo.entity.TodoRecurrenceException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TodoRecurrenceExceptionRepository extends JpaRepository<TodoRecurrenceException, Long> {

    /**
     * 반복 그룹의 모든 예외 조회
     */
    List<TodoRecurrenceException> findByTodoRecurrenceGroupId(Long groupId);

    /**
     * 반복 그룹의 특정 날짜 예외 조회
     */
    Optional<TodoRecurrenceException> findByTodoRecurrenceGroupIdAndExceptionDate(Long groupId, LocalDate date);
}
