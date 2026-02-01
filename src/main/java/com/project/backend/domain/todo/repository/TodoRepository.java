package com.project.backend.domain.todo.repository;

import com.project.backend.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    /**
     * 회원의 모든 할 일 조회
     */
    List<Todo> findByMemberId(Long memberId);

    /**
     * 회원의 완료/미완료 할 일 조회
     */
    List<Todo> findByMemberIdAndIsCompleted(Long memberId, Boolean isCompleted);

    /**
     * 회원의 특정 날짜 할 일 조회
     */
    List<Todo> findByMemberIdAndDueDate(Long memberId, LocalDate dueDate);

    /**
     * 회원의 기간 내 할 일 조회
     */
    List<Todo> findByMemberIdAndDueDateBetween(Long memberId, LocalDate start, LocalDate end);

    /**
     * 회원의 모든 할 일 삭제
     */
    @Modifying
    @Query("DELETE FROM Todo t WHERE t.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
