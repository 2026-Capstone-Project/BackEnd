package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {


}
