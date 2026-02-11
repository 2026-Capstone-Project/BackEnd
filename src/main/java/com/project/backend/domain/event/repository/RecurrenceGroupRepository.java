package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecurrenceGroupRepository extends JpaRepository<RecurrenceGroup, Long> {
    List<RecurrenceGroup> findByMemberId(Long memberId);

    @Query("""
    SELECT rg
    FROM RecurrenceGroup rg
    WHERE rg.member.id = :memberId
      AND rg.event.startTime <= :startRange
      AND (
            rg.endDate IS NULL
         OR rg.endDate >= :startRange
      )
""")
    List<RecurrenceGroup> findActiveRecurrenceGroups(
            @Param("memberId") Long memberId,
            @Param("startRange") LocalDate startRange
    );

    // 무한 반복 제외, endDate가 오늘 이후, count이면 일단 가져오기
    // TODO : 리커런스 그룹의 만료를 설정하면 쓸모없는 카운트를 안가져와도 될지 않을까?
    @Query("""
        SELECT rg
        FROM RecurrenceGroup rg
        WHERE rg.member.id = :memberId
          AND rg.endType <> com.project.backend.domain.event.enums.RecurrenceEndType.NEVER
          AND (
                rg.endType = com.project.backend.domain.event.enums.RecurrenceEndType.END_BY_COUNT
                OR rg.endDate >= :today
          )
""")
    List<RecurrenceGroup> findCandidateRecurrenceGroups(
            @Param("memberId") Long memberId,
            @Param("today") LocalDate today
    );
}
