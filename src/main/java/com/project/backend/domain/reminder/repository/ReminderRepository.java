package com.project.backend.domain.reminder.repository;

import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.enums.TargetType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    @Query("SELECT r " +
            "FROM Reminder r " +
            "WHERE r.member.id = :memberId " +
            "AND r.occurrenceTime > :currentTime")
    List<Reminder> findAllByMemberIdAndCurrentTime(
            @Param("memberId") Long memberId,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT r FROM Reminder r WHERE r.lifecycleStatus = :status")
    List<Reminder> findAllByLifecycleStatus(@Param("status") LifecycleStatus status);

    @Modifying
    @Query("DELETE FROM Reminder r WHERE r.lifecycleStatus = :status")
    void deleteByLifecycleStatus(@Param("status") LifecycleStatus status);
}
