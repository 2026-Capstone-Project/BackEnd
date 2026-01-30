package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.RecurrenceException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {
}
