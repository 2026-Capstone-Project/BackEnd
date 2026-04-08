package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.dto.EventParticipantCountProjection;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.EventParticipant;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.InviteStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    @Query("select ep.member.id from EventParticipant ep where ep.event.id = :eventId")
    List<Long> findMemberIdsByEventId(@Param("eventId") Long eventId);

    List<EventParticipant> findAllByEventId(Long eventId);

    @Query("""
    select ep
    from EventParticipant ep
    join fetch ep.event
    join fetch ep.owner
    where ep.member.id = :memberId
      and ep.status = :status
""")
    List<EventParticipant> findAllByMemberIdAndStatus(
            @Param("memberId") Long memberId, @Param("status") InviteStatus status);

    @Query("""
        select ep.event.id as eventId, count(ep) as participantCount
        from EventParticipant ep
        where ep.event.id in :eventIds and ep.status = :status
        group by ep.event.id
    """)
    List<EventParticipantCountProjection> countParticipantsByEventIds(
            @Param("eventIds") List<Long> eventIds, @Param("status") InviteStatus status);

    // 상태가 PENDING이면서, participantId로 조회
    Optional<EventParticipant> findByIdAndStatus(Long eventParticipantId, InviteStatus inviteStatus);

    @Modifying
    @Query("delete from EventParticipant ep where ep.event.id = :eventId")
    void deleteAllByEventId(Long eventId);

    // 멤버 아이디와 이벤트 아이디로 객체 조회
    Optional<EventParticipant> findByMemberIdAndEventId(Long memberId, Long eventId);

    boolean existsByEventIdAndMemberIdAndStatus(Long eventId, Long memberId, InviteStatus status);

    // 시작과 종료 범위를 만족하는 내가 공유 받은 반복하지 않는 이벤트를 검색
    @Query("SELECT DISTINCT e " +
            "FROM EventParticipant ep " +
            "JOIN ep.event e " +
            "WHERE ep.member.id = :memberId " +
            "AND ep.status = :status " +
            "AND e.recurrenceGroup IS NULL " +
            "AND e.startTime <= :endRange " +
            "AND e.endTime >= :startRange ")
    List<Event> findByMemberIdAndOverlappingRange(Long memberId, LocalDateTime startRange, LocalDateTime endRange, InviteStatus status);

    // 그룹의 모객체가 종료기간 전에 있는 공유 받은 반복하는 이벤트 검색
    @Query("SELECT DISTINCT rg " +
            "FROM EventParticipant ep " +
            "JOIN ep.event e " +
            "JOIN e.recurrenceGroup rg " +
            "WHERE ep.member.id = :memberId " +
            "AND ep.status = :status  " +
            "AND (rg.endDate IS NULL OR rg.endDate >= :startDate)"
    )
    List<RecurrenceGroup> findSharedActiveRecurrenceGroups(Long memberId, LocalDate startDate, InviteStatus status);
}
