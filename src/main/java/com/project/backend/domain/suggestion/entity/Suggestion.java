package com.project.backend.domain.suggestion.entity;

import com.project.backend.domain.event.entity.Event;
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

    @Column(name = "title", nullable = false, unique = true)
    private String title;

    @Column(name = "primary_diff", nullable = false)
    private Integer primaryDiff;

    @Column(name = "secondary_diff")
    private Integer secondaryDiff;

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
}
