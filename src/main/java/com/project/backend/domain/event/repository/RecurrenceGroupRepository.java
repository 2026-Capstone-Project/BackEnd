package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface RecurrenceGroupRepository extends JpaRepository<RecurrenceGroup, Long> {

    @Modifying
    @Query("DELETE FROM RecurrenceGroup rg WHERE rg.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
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
}
