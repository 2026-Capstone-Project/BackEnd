package com.project.backend.domain.suggestion.entity;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.suggestion.enums.SuggestionType;
import com.project.backend.domain.suggestion.vo.SuggestionPattern;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import com.project.backend.global.entity.BaseEntity;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "suggestion_type", nullable = false, length = 30)
    private SuggestionType suggestionType;

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

    @Column(name = "primary_anchor_date")
    private LocalDate primaryAnchorDate;

    @Column(name = "secondary_anchor_date")
    private LocalDate secondaryAnchorDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 시간이 지나면 같은 이전 이벤트/그룹으로 여러 Suggestion 히스토리가 생길 수 있음 -> 나중에 개발을 위해
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_event")
    private Event previousEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_todo")
    private Todo previousTodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_group")
    private RecurrenceGroup recurrenceGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_recurrence_group")
    private TodoRecurrenceGroup todoRecurrenceGroup;

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
