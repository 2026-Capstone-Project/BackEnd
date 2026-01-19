package com.project.backend.domain.event.entity;

import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.global.entity.BaseEntity;
import com.project.backend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "event")
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "location")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", nullable = false, length = 10)
    private RecurrenceFrequency recurrenceFrequency;

    @Column(name = "is_all_day", nullable = false)
    private Boolean isAllDay;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_group_id")
    private RecurrenceGroup recurrenceGroup;

    public static Event createFromNaturalLanguage(
            Member member,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            RecurrenceGroup recurrenceGroup
    ) {
        return Event.builder()
                .member(member)
                .title(title)
                .startTime(startTime)
                .endTime(endTime)
                .recurrenceFrequency(RecurrenceFrequency.NONE)
                .isAllDay(false)
                .recurrenceGroup(recurrenceGroup)
                .build();
    }

    public static Event createSingle(
            Member member,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer durationMinutes,
            boolean isAllDay
    ) {
        return Event.builder()
                .member(member)
                .title(title)
                .startTime(startTime)
                .endTime(endTime)
                .durationMinutes(durationMinutes)
                .isAllDay(isAllDay)
                .recurrenceFrequency(RecurrenceFrequency.NONE)
                .recurrenceGroup(null)
                .build();
    }

    public static Event createRecurring(
            Member member,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer durationMinutes,
            boolean isAllDay,
            RecurrenceFrequency recurrenceFrequency,
            RecurrenceGroup recurrenceGroup
    ) {
        return Event.builder()
                .member(member)
                .title(title)
                .startTime(startTime)
                .endTime(endTime)
                .durationMinutes(durationMinutes)
                .isAllDay(isAllDay)
                .recurrenceFrequency(recurrenceFrequency)
                .recurrenceGroup(recurrenceGroup)
                .build();
    }

    public boolean isRecurring() {
        return recurrenceGroup != null;
    }
}
