package com.project.backend.domain.todo.repository;

import com.project.backend.domain.todo.entity.TodoRecurrenceException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TodoRecurrenceExceptionRepository extends JpaRepository<TodoRecurrenceException, Long> {

    @Modifying
    @Query("DELETE FROM TodoRecurrenceException tre WHERE tre.todoRecurrenceGroup.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    /**
     * 반복 그룹의 모든 예외 조회
     */
    List<TodoRecurrenceException> findByTodoRecurrenceGroupId(Long groupId);

    /**
     * 반복 그룹의 특정 날짜 예외 조회
     */
    @Query("SELECT e " +
            "FROM TodoRecurrenceException e" +
            " WHERE e.todoRecurrenceGroup.id = :groupId " +
            "AND e.exceptionDate = :date")
    Optional<TodoRecurrenceException> findByTodoRecurrenceGroupIdAndExceptionDate(Long groupId, LocalDate date);

    /**
     * 반복 그룹의 특정 날짜 예외와 그 이후 occurrenceDate를 가진 예외 삭제
     */
    @Modifying
    @Query("DELETE FROM TodoRecurrenceException e " +
            "WHERE e.todoRecurrenceGroup.id = :groupId " +
            "AND e.exceptionDate >= :occurrenceDate")
    void deleteByTodoRecurrenceGroupIdAndOccurrenceDate(Long groupId, LocalDate occurrenceDate);
}
