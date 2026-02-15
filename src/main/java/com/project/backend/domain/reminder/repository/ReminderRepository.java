package com.project.backend.domain.reminder.repository;

import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.enums.ReminderRole;
import com.project.backend.domain.reminder.enums.TargetType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    /**
     * Reminder에 설정된 occurrenceTime이 현재 날짜이며, 현재시간보다 이후안 Reminder 가져오기
     */
    List<Reminder> findByMemberIdAndLifecycleStatusAndOccurrenceTimeAfterAndOccurrenceTimeLessThanEqual(
            Long memberId,
            LifecycleStatus status,
            LocalDateTime now,
            LocalDateTime endOfToday
    );

    /**
     * 라이프 사이클(활성, 비활성)에 따른 Reminder 가져오기
     */
    @Query("SELECT r FROM Reminder r WHERE r.lifecycleStatus = :status")
    List<Reminder> findAllByLifecycleStatus(@Param("status") LifecycleStatus status);

    /**
     * 타겟id와 타겟타입을 통해 Reminder 조회
     */
    @Query("SELECT r FROM Reminder r WHERE r.targetId = :targetId AND r.targetType = :targetType")
    Optional<Reminder> findByTargetIdAndTargetType(
            @Param("targetId") Long targetId,
            @Param("targetType") TargetType targetType
    );

    /**
     * 리마인더의 역할 구분 조회
     */
    @Query("SELECT r FROM Reminder r WHERE r.targetId = :targetId AND r.targetType = :type AND r.role = :role")
    Optional<Reminder> findByIdAndTypeAndRole(
            @Param("targetId") Long targetId,
            @Param("type") TargetType type,
            @Param("role") ReminderRole role
    );

    /**
     * 라이프 사이클(활성, 비활성)에 따른 Reminder 삭제
     */
    @Modifying
    @Query("DELETE FROM Reminder r WHERE r.lifecycleStatus = :status")
    void deleteByLifecycleStatus(@Param("status") LifecycleStatus status);

    /**
     * occurrenceTime와 일치하는 날짜값을 가진 Reminder 삭제
     */
    @Modifying
    @Query("DELETE FROM Reminder r " +
            "WHERE r.targetId = :targetId " +
            "AND r.targetType = :type " +
            "AND r.occurrenceTime >= :occurrenceTime")
    void deleteByTargetIdAndTargetTypeAndOccurrenceTime(
            @Param("targetId") Long targetId,
            @Param("type") TargetType type,
            @Param("occurrenceTime") LocalDateTime occurrenceTime
    );

    /**
     * occurrenceTime보다 이후 날짜값을 가진 Reminder 삭제
     */
    @Modifying
    @Query("DELETE FROM Reminder r " +
            "WHERE r.targetId = :targetId " +
            "AND r.targetType = :type " +
            "AND r.occurrenceTime >= :occurrenceTime")
    void deleteRemindersFromOccurrenceTime(
            @Param("targetId") Long targetId,
            @Param("type") TargetType type,
            @Param("occurrenceTime") LocalDateTime occurrenceTime
    );

    Optional<Reminder> findByRecurrenceExceptionIdAndTargetType(Long id, TargetType type);

    void deleteByTargetIdAndTargetType(Long targetId, TargetType type);
}
