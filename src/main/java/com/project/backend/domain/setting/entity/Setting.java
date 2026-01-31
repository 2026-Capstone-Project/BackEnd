package com.project.backend.domain.setting.entity;

import com.project.backend.global.entity.BaseEntity;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.setting.enums.DefaultView;
import com.project.backend.domain.setting.enums.ReminderTiming;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "setting")
public class Setting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "daily_briefing", nullable = false)
    private Boolean dailyBriefing = true;

    @Builder.Default
    @Column(name = "briefing_time")
    private LocalTime dailyBriefingTime = LocalTime.of(8, 0);

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_timing", nullable = false, length = 20)
    private ReminderTiming reminderTiming = ReminderTiming.ONE_HOUR;

    @Builder.Default
    @Column(name = "suggestion", nullable = false)
    private Boolean suggestion = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "default_view", nullable = false, length = 20)
    private DefaultView defaultView = DefaultView.MONTH;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    // update 메서드
    public void toggleDailyBriefing() {
        this.dailyBriefing = !this.dailyBriefing;
    }

    public void updateDailyBriefingTime(LocalTime newDailyBriefingTime) {
        this.dailyBriefingTime = newDailyBriefingTime;
    }

    public void updateReminderTiming(ReminderTiming newReminderTiming) {
        this.reminderTiming = newReminderTiming;
    }

    public void toggleSuggestion() {
        this.suggestion = !this.suggestion;
    }

    public void updateDefaultView(DefaultView newDefaultView) {
        this.defaultView = newDefaultView;
    }
}
