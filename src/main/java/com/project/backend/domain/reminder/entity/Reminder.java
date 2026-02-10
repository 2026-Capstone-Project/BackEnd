package com.project.backend.domain.reminder.entity;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.reminder.enums.InteractionStatus;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.enums.ReminderRole;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Reminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    // 해당 일정/할일의 다음 발생 시간이자 리마인더 기준 시간
    @Column(name = "occurrence_time", nullable = false)
    private LocalDateTime occurrenceTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_status", nullable = false)
    private InteractionStatus interactionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false)
    private LifecycleStatus lifecycleStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ReminderRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public void terminate() {
        this.lifecycleStatus = LifecycleStatus.TERMINATED;
    }

    public void inActive() {
        this.lifecycleStatus = LifecycleStatus.INACTIVE;
    }

    public void updateOccurrence(LocalDateTime time) {
        this.occurrenceTime = time;
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}
