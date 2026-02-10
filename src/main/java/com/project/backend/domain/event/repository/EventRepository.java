package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Modifying
    @Query("DELETE FROM Event e WHERE e.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    Optional<Event> findByMemberIdAndId(Long memberId, Long eventId);

    List<Event> findByMemberIdAndStartTimeBetween(Long memberId, LocalDateTime start, LocalDateTime end);

    List<Event> findByRecurrenceGroup(RecurrenceGroup recurrenceGroup);

    void deleteByRecurrenceGroup(RecurrenceGroup recurrenceGroup);

    @Query("SELECT e " +
            "FROM Event e " +
            "WHERE e.member.id = :memberId " +
            "AND e.startTime <= :endRange " +
            "AND e.endTime >= :startRange ")
    List<Event> findByMemberIdAndOverlappingRange(
            @Param("memberId") Long memberId,
            @Param("startRange") LocalDateTime startRange,
            @Param("endRange") LocalDateTime endRange);

    List<Event> findAllByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT e " +
            "FROM Event e " +
            "WHERE e.member.id = :memberId AND e.startTime <= :currentDate")
    List<Event> findAllByMemberIdAndCurrentDate(
            @Param("memberId") Long memberId,
            @Param("currentDate") LocalDateTime currentDate
    );
}
