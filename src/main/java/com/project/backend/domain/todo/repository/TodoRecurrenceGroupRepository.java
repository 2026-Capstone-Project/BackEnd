package com.project.backend.domain.todo.repository;

import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TodoRecurrenceGroupRepository extends JpaRepository<TodoRecurrenceGroup, Long> {

    /**
     * 회원의 모든 반복 할 일 그룹 조회
     */
    List<TodoRecurrenceGroup> findByMemberId(Long memberId);

    @Modifying
    @Query("DELETE FROM TodoRecurrenceGroup trg WHERE trg.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
