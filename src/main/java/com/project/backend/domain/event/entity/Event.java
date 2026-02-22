package com.project.backend.domain.event.entity;

import com.project.backend.domain.event.enums.EventColor;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.global.entity.BaseEntity;
import com.project.backend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
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

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "location")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", nullable = false, length = 10)
    private RecurrenceFrequency recurrenceFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false, length = 10)
    @Builder.Default
    private EventColor color = EventColor.BLUE;

    @Column(name = "is_all_day", nullable = false)
    private Boolean isAllDay;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "source_suggestion_id")
    private Long sourceSuggestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_group_id")
    private RecurrenceGroup recurrenceGroup;

    public static Event createFromNaturalLanguage(
            Member member,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            RecurrenceGroup recurrenceGroup,
            EventColor color
    ) {
        return Event.builder()
                .member(member)
                .title(title)
                .startTime(startTime)
                .endTime(endTime)
                .recurrenceFrequency(RecurrenceFrequency.NONE)
                .color(color != null ? color : EventColor.BLUE)
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
            boolean isAllDay,
            EventColor color
    ) {
        return Event.builder()
                .member(member)
                .title(title)
                .startTime(startTime)
                .endTime(endTime)
                .durationMinutes(durationMinutes)
                .isAllDay(isAllDay)
                .recurrenceFrequency(RecurrenceFrequency.NONE)
                .color(color != null ? color : EventColor.BLUE)
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
            RecurrenceGroup recurrenceGroup,
            EventColor color
    ) {
        return Event.builder()
                .member(member)
                .title(title)
                .startTime(startTime)
                .endTime(endTime)
                .durationMinutes(durationMinutes)
                .isAllDay(isAllDay)
                .recurrenceFrequency(recurrenceFrequency)
                .color(color != null ? color : EventColor.BLUE)
                .recurrenceGroup(recurrenceGroup)
                .build();
    }

    public boolean isRecurring() {
        return recurrenceGroup != null;
    }

    public void update(
            String title,
            String content,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location,
            EventColor color,
            Boolean isAllDay
    ) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (startTime != null) {
            this.startTime = startTime;
            this.endTime = startTime.plusMinutes(durationMinutes);
        }
        if (endTime != null) this.endTime = endTime;
        if (location != null) this.location = location;
        if (color != null) this.color = color;
        if (isAllDay != null) this.isAllDay = isAllDay;
        if (startTime != null && endTime != null) {
            this.durationMinutes = (int) Duration.between(startTime, endTime).toMinutes();
        }
    }

    public void updateRecurrenceGroup(RecurrenceGroup recurrenceGroup) {
        this.recurrenceGroup = recurrenceGroup;
        this.recurrenceFrequency = recurrenceGroup.getFrequency();
    }
}
