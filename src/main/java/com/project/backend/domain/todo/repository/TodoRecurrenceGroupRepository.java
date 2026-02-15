package com.project.backend.domain.todo.repository;

import com.project.backend.domain.todo.entity.TodoRecurrenceException;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TodoRecurrenceGroupRepository extends JpaRepository<TodoRecurrenceGroup, Long> {

    /**
     * 회원의 모든 반복 할 일 그룹 조회
     */
    List<TodoRecurrenceGroup> findByMemberId(Long memberId);


    /**
     * 현재 활성화된 반복 그룹 조회
     */
    // 무한 반복 제외, endDate가 오늘 이후, count이면 일단 가져오기
    // TODO : 리커런스 그룹의 만료를 설정하면 쓸모없는 카운트를 안가져와도 될지 않을까?
    @Query("""
        SELECT trg
        FROM TodoRecurrenceGroup trg
        WHERE trg.member.id = :memberId
          AND trg.endType <> com.project.backend.domain.event.enums.RecurrenceEndType.NEVER
          AND (
                trg.endType = com.project.backend.domain.event.enums.RecurrenceEndType.END_BY_COUNT
                OR trg.endDate >= :today
          )
""")
    List<TodoRecurrenceGroup> findCandidateTodoRecurrenceGroups(
            @Param("memberId") Long memberId,
            @Param("today") LocalDate today
    );

    @Modifying
    @Query("DELETE FROM TodoRecurrenceGroup trg WHERE trg.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
