package com.project.backend.domain.suggestion.entity;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.suggestion.vo.SuggestionPattern;
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
@Table(name = "suggestion")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_event", nullable = false)
    private Event previousEvent;

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
}
