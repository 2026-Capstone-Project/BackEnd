package com.project.backend.domain.todo.entity;

import com.project.backend.domain.event.enums.ExceptionType;
import com.project.backend.domain.todo.enums.Priority;
import com.project.backend.domain.todo.enums.TodoColor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "todo_recurrence_exception",
        // 같은 반복 그룹에서 같은 날짜의 예외는 딱 하나만 존재할 수 있음
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"todo_recurrence_group_id", "exception_date"}
                )
        })
public class TodoRecurrenceException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 10)
    private ExceptionType exceptionType;

    // ===== OVERRIDE일 때만 사용 =====
    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "due_time")
    private LocalTime dueTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", length = 10)
    private TodoColor color;

    @Column(name = "memo")
    private String memo;
    // ==============================

    // Todo 특화 필드: 특정 날짜의 완료 상태
    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_recurrence_group_id", nullable = false)
    private TodoRecurrenceGroup todoRecurrenceGroup;

    // ===== 비즈니스 메서드 =====

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
     * OVERRIDE 예외 정보 수정
     */
    public void updateOverride(String title, LocalTime dueTime, Priority priority, TodoColor color, String memo) {
        this.exceptionType = ExceptionType.OVERRIDE;
        this.title = title;
        this.dueTime = dueTime;
        this.priority = priority;
        this.color = color;
        this.memo = memo;
    }

    // ===== 팩토리 메서드 =====

    /**
     * SKIP 예외 생성 (특정 날짜 건너뛰기)
     */
    public static TodoRecurrenceException createSkip(
            TodoRecurrenceGroup todoRecurrenceGroup,
            LocalDate exceptionDate
    ) {
        return TodoRecurrenceException.builder()
                .todoRecurrenceGroup(todoRecurrenceGroup)
                .exceptionDate(exceptionDate)
                .exceptionType(ExceptionType.SKIP)
                .isCompleted(false)
                .build();
    }

    /**
     * OVERRIDE 예외 생성 (특정 날짜 수정)
     */
    public static TodoRecurrenceException createOverride(
            TodoRecurrenceGroup todoRecurrenceGroup,
            LocalDate exceptionDate,
            String title,
            LocalTime dueTime,
            Priority priority,
            TodoColor color,
            String memo
    ) {
        return TodoRecurrenceException.builder()
                .todoRecurrenceGroup(todoRecurrenceGroup)
                .exceptionDate(exceptionDate)
                .exceptionType(ExceptionType.OVERRIDE)
                .title(title)
                .dueTime(dueTime)
                .priority(priority)
                .color(color)
                .memo(memo)
                .isCompleted(false)
                .build();
    }

    /**
     * 완료 상태만 기록하는 예외 생성
     */
    public static TodoRecurrenceException createCompleted(
            TodoRecurrenceGroup todoRecurrenceGroup,
            LocalDate exceptionDate
    ) {
        return TodoRecurrenceException.builder()
                .todoRecurrenceGroup(todoRecurrenceGroup)
                .exceptionDate(exceptionDate)
                .exceptionType(ExceptionType.OVERRIDE)
                .isCompleted(true)
                .build();
    }

    /**
     * ExceptionType을 skip으로 업데이트
     */
    public void updateExceptionTypeToSKIP() {
        this.exceptionType = ExceptionType.SKIP;
    }

    public LocalDateTime getExceptionDateTime() {
        return LocalDateTime.of(exceptionDate, dueTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TodoRecurrenceException other)) return false;
        return exceptionDate.equals(other.exceptionDate)
                && todoRecurrenceGroup.equals(other.todoRecurrenceGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(todoRecurrenceGroup, exceptionDate);
    }
}
