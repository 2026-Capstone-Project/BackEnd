package com.project.backend.domain.event.entity;

import com.project.backend.domain.event.enums.EventColor;
import com.project.backend.domain.event.enums.ExceptionType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Table(name = "recurrence_exception",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"recurrence_group_id", "exception_date"}
                )
        })
public class RecurrenceException extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exception_date", nullable = false)
    private LocalDateTime exceptionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 10)
    private ExceptionType exceptionType;

    // ===== OVERRIDE일 때만 사용 =====
    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "location")
    private String location;

    // TODO 리팩토링 단계에서 삭제할지 말지 결정
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", length = 10)
    private RecurrenceFrequency recurrenceFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", length = 10)
    @Builder.Default
    private EventColor color = EventColor.BLUE;

    @Column(name = "is_all_day")
    private Boolean isAllDay;
    // ==============================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_group_id", nullable = false)
    private RecurrenceGroup recurrenceGroup;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecurrenceException other)) return false;
        return exceptionDate.equals(other.exceptionDate)
                && recurrenceGroup.equals(other.recurrenceGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recurrenceGroup, exceptionDate);
    }

    public void updateExceptionTypeToSKIP() {
        this.exceptionType = ExceptionType.SKIP;
    }

    public void updateForUpdate(
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
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (location != null) this.location = location;
        if (color != null) this.color = color;
        if (isAllDay != null) this.isAllDay = isAllDay;
    }
}
