package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.dto.EventParticipantCountProjection;
import com.project.backend.domain.event.entity.EventParticipant;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    @Query("select ep.member.id from EventParticipant ep where ep.event.id = :eventId")
    List<Long> findMemberIdsByEventId(@Param("eventId") Long eventId);

    List<EventParticipant> findAllByEventId(Long eventId);

    List<EventParticipant> findAllByMemberId(Long memberId);

    @Query("""
        select ep.event.id as eventId, count(ep) as participantCount
        from EventParticipant ep
        where ep.event.id in :eventIds
        group by ep.event.id
    """)
    List<EventParticipantCountProjection> countParticipantsByEventIds(@Param("eventIds") List<Long> eventIds);

}
