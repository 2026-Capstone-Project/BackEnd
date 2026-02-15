package com.project.backend.domain.suggestion.vo;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import com.project.backend.global.recurrence.RecurrenceRule;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecurrenceSuggestionCandidate(
        Long id,
        RecurrenceFrequency frequency,
        LocalDateTime startDate,
        RecurrenceEndType endType,
        LocalDate endDate,
        Integer occurrenceCount,

        Category category,
        Member member,
        Event event,
        Todo todo,

        Integer intervalValue,

        String daysOfWeek,
        MonthlyType monthlyType,
        String daysOfMonth,
        Integer weekOfMonth,
        String dayOfWeekInMonth,
        Integer monthOfYear
) implements RecurrenceRule {

    public static RecurrenceSuggestionCandidate from(RecurrenceGroup rg) {
        return new RecurrenceSuggestionCandidate(
                rg.getId(),
                rg.getFrequency(),
                rg.getEvent().getStartTime(),
                rg.getEndType(),
                rg.getEndDate(),
                rg.getOccurrenceCount(),
                Category.EVENT,
                rg.getMember(),
                rg.getEvent(),
                null,
                rg.getIntervalValue(),
                rg.getDaysOfWeek(),
                rg.getMonthlyType(),
                rg.getDaysOfMonth(),
                rg.getWeekOfMonth(),
                rg.getDayOfWeekInMonth(),
                rg.getMonthOfYear()
        );
    }

    public static RecurrenceSuggestionCandidate from(TodoRecurrenceGroup trg) {
        return new RecurrenceSuggestionCandidate(
                trg.getId(),
                trg.getFrequency(),
                trg.getTodo().getStartDate().atTime(trg.getTodo().getDueTime()),
                trg.getEndType(),
                trg.getEndDate(),
                trg.getOccurrenceCount(),
                Category.TODO,
                trg.getMember(),
                null,
                trg.getTodo(),
                trg.getIntervalValue(),
                trg.getDaysOfWeek(),
                trg.getMonthlyType(),
                trg.getDaysOfMonth(),
                trg.getWeekOfMonth(),
                trg.getDayOfWeekInMonth(),
                trg.getMonthOfYear()
        );
    }

    public String title() {
        return this.category == Category.EVENT ? event.getTitle() : todo.getTitle();
    }

    @Override
    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    @Override
    public Integer getIntervalValue() {
        return intervalValue;
    }

    @Override
    public RecurrenceEndType getEndType() {
        return endType;
    }

    @Override
    public LocalDate getEndDate() {
        return endDate;
    }

    @Override
    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }

    @Override
    public String getDaysOfWeek() {
        return daysOfWeek();
    }

    @Override
    public MonthlyType getMonthlyType() {
        return monthlyType();
    }

    @Override
    public String getDaysOfMonth() {
        return daysOfMonth();
    }

    @Override
    public Integer getWeekOfMonth() {
        return weekOfMonth();
    }

    @Override
    public String getDayOfWeekInMonth() {
        return dayOfWeekInMonth();
    }

    @Override
    public Integer getMonthOfYear() {
        return monthOfYear();
    }
}
