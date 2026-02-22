package com.project.backend.domain.event.entity;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.common.plan.enums.MonthlyWeekdayRule;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.global.entity.BaseEntity;
import com.project.backend.global.recurrence.RecurrenceRule;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "recurrence_group")
public class RecurrenceGroup extends BaseEntity implements RecurrenceRule {

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

    @Column(name = "day_of_week_in_month", length = 60)
    private String dayOfWeekInMonth;

    @Column(name = "month_of_year")
    private Integer monthOfYear;

    @Column(name = "is_custom")
    private Boolean isCustom;

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

    @OneToMany(mappedBy = "recurrenceGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RecurrenceException> exceptionDates = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

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

    public void setEvent(Event event) {
        this.event = event;
    }
  
    public void addExceptionDate(RecurrenceException exceptionDate) {
        exceptionDates.add(exceptionDate);
    }

    public void updateEndDateTime(LocalDateTime endDate) {
        this.endType = RecurrenceEndType.END_BY_DATE;
        this.endDate = endDate.toLocalDate().minusDays(1);
    }

    public void updateEvent(Event event) {
        this.event = event;
    }

    public List<Integer> getDaysOfMonthAsList() {
        if (daysOfMonth == null) return null;
        return Arrays.stream(daysOfMonth.split(","))
                .map(Integer::valueOf)
                .toList();
    }

    public void extendEndDate(long dayDiff) {
        this.endDate = this.endDate.plusDays(dayDiff);
    }

    public void updateOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }
      
    public void attachEvent(Event event) {
        this.event = event;
    }

}
