package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.RecurrenceException;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {

    List<RecurrenceException> findByRecurrenceGroupId(Long recurrenceGroupId);
}
