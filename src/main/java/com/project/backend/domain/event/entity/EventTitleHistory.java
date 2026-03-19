package com.project.backend.domain.event.entity;

import com.project.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "event_title_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_title_history_member_title",
                        columnNames = {"member_id", "title"}
                )
        }
//        indexes = {
//                @Index(
//                        name = "idx_event_title_history_member_last_used_at",
//                        columnList = "member_id, last_used_at"
//                )
//        }
)
public class EventTitleHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
