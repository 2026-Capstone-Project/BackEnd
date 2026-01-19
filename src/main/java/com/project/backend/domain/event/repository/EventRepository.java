package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Modifying
    @Query("DELETE FROM Event e WHERE e.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    List<Event> findByMemberIdAndStartTimeBetween(Long memberId, LocalDateTime start, LocalDateTime end);

    List<Event> findByRecurrenceGroup(RecurrenceGroup recurrenceGroup);

    void deleteByRecurrenceGroup(RecurrenceGroup recurrenceGroup);
}
