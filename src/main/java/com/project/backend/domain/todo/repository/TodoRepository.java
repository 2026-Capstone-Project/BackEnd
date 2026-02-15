package com.project.backend.domain.todo.repository;

import com.project.backend.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
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
    List<Todo> findByMemberIdAndStartDate(Long memberId, LocalDate startDate);

    /**
     * 회원의 기간 내 할 일 조회
     */
    List<Todo> findByMemberIdAndStartDateBetween(Long memberId, LocalDate start, LocalDate end);

    /**
     * 회원의 모든 할 일 삭제
     */
    @Modifying
    @Query("UPDATE Todo t SET t.todoRecurrenceGroup = NULL WHERE t.member.id = :memberId")
    void clearTodoRecurrenceGroupByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM Todo t WHERE t.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    /**
     * 회원의 반복하지 않는 할 일 조회
     */
    @Query("SELECT t " +
            "FROM Todo t " +
            "WHERE t.member.id = :memberId " +
            "AND t.startDate >= :from " +
            "AND t.startDate <= :to " +
            "AND t.todoRecurrenceGroup IS NULL " +
            "ORDER BY t.startDate")
    List<Todo> findByMemberIdAndInRangeAndRecurrenceGroupIsNull(
            @Param("memberId") Long memberId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    
    /**
     * 오늘보다 startTime이 이후가 아닌 할일 조회
     */
    @Query("SELECT t " +
            "FROM Todo t " +
            "WHERE t.member.id = :memberId " +
            "AND t.startDate <= :currentDate")
    List<Todo> findAllByMemberIdAndCurrentDate(
            @Param("memberId") Long memberId,
            @Param("currentDate") LocalDate currentDate
    );
  
    /**
     * 이미 존재하는 Todo 인가
     */
    boolean existsByMemberIdAndTitleAndMemoAndStartDateAndDueTime(Long memberId, String title, String memo, LocalDate startDate, LocalTime dueTime);

}
