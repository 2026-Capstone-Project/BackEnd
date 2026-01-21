package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurrenceGroupRepository extends JpaRepository<RecurrenceGroup, Long> {
    List<RecurrenceGroup> findByMemberId(Long memberId);
}
