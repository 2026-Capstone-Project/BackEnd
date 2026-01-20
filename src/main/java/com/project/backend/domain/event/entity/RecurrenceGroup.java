package com.project.backend.domain.event.entity;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "recurrence_group")
public class RecurrenceGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private RecurrenceFrequency frequency;

    @Column(name = "interval_value", nullable = false)
    private Integer intervalValue;

    // WEEKLY: 반복 요일 (JSON 문자열)
    @Column(name = "days_of_week", length = 100)
    private String daysOfWeek;

    // MONTHLY: 반복 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "monthly_type", length = 20)
    private MonthlyType monthlyType;

    @Column(name = "days_of_month", length = 100)
    private String daysOfMonth;

    @Column(name = "week_of_month")
    private Integer weekOfMonth;

    @Column(name = "day_of_week_in_month", length = 10)
    private String dayOfWeekInMonth;

    // YEARLY: 반복 월
    @Column(name = "month_of_year")
    private Integer monthOfYear;

    // 종료 조건
    @Enumerated(EnumType.STRING)
    @Column(name = "end_type", nullable = false, length = 20)
    private RecurrenceEndType endType;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    @Column(name = "created_count", nullable = false)
    private Integer createdCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public static RecurrenceGroup create(
            Member member,
            RecurrenceFrequency frequency,
            Integer intervalValue,
            String daysOfWeek,
            MonthlyType monthlyType,
            String daysOfMonth,
            Integer weekOfMonth,
            String dayOfWeekInMonth,
            Integer monthOfYear,
            RecurrenceEndType endType,
            LocalDate endDate,
            Integer occurrenceCount,
            Integer createdCount
    ) {
        return RecurrenceGroup.builder()
                .member(member)
                .frequency(frequency)
                .intervalValue(intervalValue != null ? intervalValue : 1)
                .daysOfWeek(daysOfWeek)
                .monthlyType(monthlyType)
                .daysOfMonth(daysOfMonth)
                .weekOfMonth(weekOfMonth)
                .dayOfWeekInMonth(dayOfWeekInMonth)
                .monthOfYear(monthOfYear)
                .endType(endType != null ? endType : RecurrenceEndType.NEVER)
                .endDate(endDate)
                .occurrenceCount(occurrenceCount)
                .createdCount(createdCount)
                .build();
    }
}
