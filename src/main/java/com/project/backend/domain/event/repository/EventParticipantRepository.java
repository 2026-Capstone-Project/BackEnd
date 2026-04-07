package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.dto.EventParticipantCountProjection;
import com.project.backend.domain.event.entity.EventParticipant;
import com.project.backend.domain.event.enums.InviteStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

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

}
