package com.project.backend.domain.suggestion.entity;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.suggestion.vo.SuggestionPattern;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.global.entity.BaseEntity;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "suggestion",
        uniqueConstraints = {
                // "활성 제안(active=true)"은 (memberId, targetKeyHash) 당 1개만
                @UniqueConstraint(
                        name = "uk_suggestion_active_target",
                        columnNames = {"member_id", "target_key_hash", "active"}
                )
        }
)
public class Suggestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "primary_content", nullable = false)
    private String primaryContent;

    @Column(name = "secondary_content")
    private String secondaryContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    /**
     * status랑 별개로 "현재 유효/노출/수락 가능한 row인지"를 DB 유니크와 조회에 쓰는 스위치
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * 유니크 타겟 키(단발성/반복그룹/투두 모두 통일)
     * - 단발성(이벤트 그룹): "E|{normalizedTitle}|{normalizedLocation}"
     * - 반복그룹 연장      : "RG|{recurrenceGroupId}"
     * - 투두(추후)         : "T|{normalizedTitle}"
     */
    @Column(name = "target_key", nullable = false, length = 300)
    private String targetKey;

    @Column(name = "target_key_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] targetKeyHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 시간이 지나면 같은 이전 이벤트/그룹으로 여러 Suggestion 히스토리가 생길 수 있음 -> 나중에 개발을 위해
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "previous_event")
    private Event previousEvent;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "previous_todo")
    private Todo previousTodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_group")
    private RecurrenceGroup recurrenceGroup;

    // TODO : 이것이 최선인가?
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dayDiff", column = @Column(name = "primary_day_diff")),
            @AttributeOverride(name = "weekDiff", column = @Column(name = "primary_week_diff")),
            @AttributeOverride(name = "monthDiff", column = @Column(name = "primary_month_diff")),
            @AttributeOverride(name = "dayOfWeekSet", column = @Column(name = "primary_day_of_week_set")),
            @AttributeOverride(name = "dayOfMonthSet", column = @Column(name = "primary_day_of_month_set"))
    })
    private SuggestionPattern primaryPattern;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dayDiff", column = @Column(name = "secondary_day_diff")),
            @AttributeOverride(name = "weekDiff", column = @Column(name = "secondary_week_diff")),
            @AttributeOverride(name = "monthDiff", column = @Column(name = "secondary_month_diff")),
            @AttributeOverride(name = "dayOfWeekSet", column = @Column(name = "secondary_day_of_week_set")),
            @AttributeOverride(name = "dayOfMonthSet", column = @Column(name = "secondary_day_of_month_set"))
    })
    private SuggestionPattern secondaryPattern;

    // Update 메서드

    public void accept() {
        this.status = Status.ACCEPTED;
        this.active = false;
    }

    public void reject() {
        if (this.status.equals(Status.PRIMARY) && this.secondaryContent != null) {
            this.status = Status.SECONDARY;
        }
        else {
            this.status = Status.REJECTED;
            this.active = false;
        }

    }
}
