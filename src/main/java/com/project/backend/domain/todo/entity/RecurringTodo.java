package com.project.backend.domain.todo.entity;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.todo.enums.Priority;
import com.project.backend.domain.todo.enums.RecurrenceType;
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
@Table(name = "recurring_todo")
public class RecurringTodo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false)
    private RecurrenceType recurrenceType;

    @Column(name = "custom_days", columnDefinition = "JSON")
    private String customDays;  // JSON 문자열로 저장 ["MON", "WED"] 또는 [16, 31]

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "repeat_count")
    private Integer repeatCount;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 비활성화
    public void deactivate() {
        this.isActive = false;
    }
}
