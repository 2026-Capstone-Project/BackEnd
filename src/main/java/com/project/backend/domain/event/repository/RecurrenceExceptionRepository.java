package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.enums.ExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {

    List<RecurrenceException> findAllByRecurrenceGroupId(Long recurrenceGroupId);

    Optional<RecurrenceException> findByRecurrenceGroupId(Long recurrenceGroupId);

    @Query("SELECT re " +
            "FROM RecurrenceException re " +
            "WHERE re.recurrenceGroup.id = :recurrenceGroupId " +
            "AND re.exceptionDate = :exceptionDate " +
            "AND re.exceptionType = :exceptionType")
    Optional<RecurrenceException> findByRecurrenceGroupIdAndExceptionDateAndExceptionType (
            @Param("recurrenceGroupId") Long recurrenceGroupId,
            @Param("exceptionDate") LocalDateTime exceptionDate,
            @Param("exceptionType") ExceptionType exceptionType
            );

    @Query("SELECT re " +
            "FROM RecurrenceException re " +
            "WHERE re.recurrenceGroup.id = :recurrenceGroupId " +
            "AND re.exceptionDate = :exceptionDate")
    Optional<RecurrenceException> findByRecurrenceGroupIdAndExceptionDate(
            @Param("recurrenceGroupId") Long recurrenceGroupId,
            @Param("exceptionDate") LocalDateTime exceptionDate
            );


    /**
    * occurrenceDate과 exceptionDate가 같거나 이후인 Recurrence-groupException 객체 삭제
    **/
    @Modifying
    @Query("DELETE FROM RecurrenceException re " +
            "WHERE re.recurrenceGroup.id = :recurrenceGroupId " +
            "AND re.exceptionDate >= :occurrenceDate")
    void deleteByRecurrenceGroupIdAndOccurrenceDate(
            @Param("recurrenceGroupId") Long recurrenceGroupId,
            @Param("occurrenceDate") LocalDateTime occurrenceDate
    );
}
