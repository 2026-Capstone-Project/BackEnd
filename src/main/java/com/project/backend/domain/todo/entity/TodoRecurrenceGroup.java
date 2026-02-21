package com.project.backend.domain.todo.entity;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.global.entity.BaseEntity;
import com.project.backend.global.recurrence.RecurrenceRule;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "todo_recurrence_group")
public class TodoRecurrenceGroup extends BaseEntity implements RecurrenceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private RecurrenceFrequency frequency;

    @Column(name = "interval_value", nullable = false)
    private Integer intervalValue;

    // WEEKLY: 반복 요일 (예: "MONDAY,WEDNESDAY,FRIDAY")
    @Column(name = "days_of_week", length = 100)
    private String daysOfWeek;

    // MONTHLY: 반복 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "monthly_type", length = 20)
    private MonthlyType monthlyType;

    // MONTHLY (DAY_OF_MONTH): 반복 일자 (예: "1,15")
    @Column(name = "days_of_month", length = 100)
    private String daysOfMonth;

    // MONTHLY (DAY_OF_WEEK): N번째 주
    @Column(name = "week_of_month")
    private Integer weekOfMonth;

    // MONTHLY (DAY_OF_WEEK): 반복 요일 (예: "MONDAY")
    @Column(name = "day_of_week_in_month", length = 20)
    private String dayOfWeekInMonth;

    // 종료 조건
    @Enumerated(EnumType.STRING)
    @Column(name = "end_type", nullable = false, length = 20)
    private RecurrenceEndType endType;

    // END_BY_DATE: 종료 날짜
    @Column(name = "end_date")
    private LocalDate endDate;

    // END_BY_COUNT: 반복 횟수
    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    // 예외 날짜들
    @Builder.Default
    @OneToMany(mappedBy = "todoRecurrenceGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TodoRecurrenceException> exceptionDates = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id")
    private Todo todo;

    // ===== RecurrenceRule 인터페이스 구현 =====

    /**
     * Todo에서는 YEARLY 반복 시 시작일(startDate)의 월/일을 사용하므로
     * monthOfYear 필드를 사용하지 않음
     */
    @Override
    public Integer getMonthOfYear() {
        return null;
    }

    // ===== 비즈니스 메서드 =====

    /**
     * 기본 할 일 설정
     */
    public void setTodo(Todo todo) {
        this.todo = todo;
    }

    /**
     * 예외 날짜 추가
     */
    public void addExceptionDate(TodoRecurrenceException exception) {
        this.exceptionDates.add(exception);
    }

    /**
     * 종료 날짜로 종료 조건 변경
     */
    public void updateEndByDate(LocalDate endDate) {
        this.endType = RecurrenceEndType.END_BY_DATE;
        this.endDate = endDate;
    }
    /**
     * 제안 수락 시 종료 기간 연장
     */
    public void extendEndDate(long dayDiff) {
        this.endDate = endDate.plusDays(dayDiff);
    }
    /**
     * 제안 수락 시 반복 횟수 추가
     */
    public void updateOccurrenceCount(Integer occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }


    // ===== 팩토리 메서드 =====

    public static TodoRecurrenceGroup create(
            Member member,
            RecurrenceFrequency frequency,
            Integer intervalValue,
            String daysOfWeek,
            MonthlyType monthlyType,
            String daysOfMonth,
            Integer weekOfMonth,
            String dayOfWeekInMonth,
            RecurrenceEndType endType,
            LocalDate endDate,
            Integer occurrenceCount
    ) {
        return TodoRecurrenceGroup.builder()
                .member(member)
                .frequency(frequency)
                .intervalValue(intervalValue != null ? intervalValue : 1)
                .daysOfWeek(daysOfWeek)
                .monthlyType(monthlyType)
                .daysOfMonth(daysOfMonth)
                .weekOfMonth(weekOfMonth)
                .dayOfWeekInMonth(dayOfWeekInMonth)
                .endType(endType != null ? endType : RecurrenceEndType.NEVER)
                .endDate(endDate)
                .occurrenceCount(occurrenceCount)
                .build();
    }
}
