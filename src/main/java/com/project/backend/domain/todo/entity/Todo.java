package com.project.backend.domain.todo.entity;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.todo.enums.Priority;
import com.project.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "todo")
public class Todo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "due_time")
    private LocalTime dueTime;

    @Builder.Default
    @Column(name = "is_all_day", nullable = false)
    private Boolean isAllDay = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "memo")
    private String memo;

    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "source_suggestion_id")
    private Long sourceSuggestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_recurrence_group_id")
    private TodoRecurrenceGroup todoRecurrenceGroup;

    // ===== 비즈니스 메서드 =====

    /**
     * 반복 할 일 여부 확인
     */
    public boolean isRecurring() {
        return todoRecurrenceGroup != null;
    }

    /**
     * 완료 처리
     */
    public void complete() {
        this.isCompleted = true;
    }

    /**
     * 미완료 처리
     */
    public void incomplete() {
        this.isCompleted = false;
    }

    /**
     * 할 일 정보 수정
     */
    public void update(String title, LocalDate startDate, LocalTime dueTime,
                       Boolean isAllDay, Priority priority, String memo) {
        if (title != null) this.title = title;
        if (startDate != null) this.startDate = startDate;
        this.dueTime = dueTime;
        if (isAllDay != null) this.isAllDay = isAllDay;
        if (priority != null) this.priority = priority;
        this.memo = memo;
    }

    /**
     * 반복 그룹 설정
     */
    public void setTodoRecurrenceGroup(TodoRecurrenceGroup todoRecurrenceGroup) {
        this.todoRecurrenceGroup = todoRecurrenceGroup;
    }

    // ===== 팩토리 메서드 =====

    /**
     * 단발성 할 일 생성
     */
    public static Todo createSingle(
            Member member,
            String title,
            LocalDate startDate,
            LocalTime dueTime,
            Boolean isAllDay,
            Priority priority,
            String memo
    ) {
        return Todo.builder()
                .member(member)
                .title(title)
                .startDate(startDate)
                .dueTime(dueTime)
                .isAllDay(isAllDay != null ? isAllDay : false)
                .priority(priority != null ? priority : Priority.MEDIUM)
                .memo(memo)
                .isCompleted(false)
                .build();
    }

    /**
     * 반복 할 일 생성
     */
    public static Todo createRecurring(
            Member member,
            String title,
            LocalDate startDate,
            LocalTime dueTime,
            Boolean isAllDay,
            Priority priority,
            String memo,
            TodoRecurrenceGroup todoRecurrenceGroup
    ) {
        return Todo.builder()
                .member(member)
                .title(title)
                .startDate(startDate)
                .dueTime(dueTime)
                .isAllDay(isAllDay != null ? isAllDay : false)
                .priority(priority != null ? priority : Priority.MEDIUM)
                .memo(memo)
                .isCompleted(false)
                .todoRecurrenceGroup(todoRecurrenceGroup)
                .build();
    }
}
