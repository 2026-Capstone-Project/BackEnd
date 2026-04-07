package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.EventParticipant;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    @Query("select ep.member.id from EventParticipant ep where ep.event.id = :eventId")
    List<Long> findMemberIdsByEventId(@Param("eventId") Long eventId);

    List<EventParticipant> findAllByEventId(Long eventId);
}
