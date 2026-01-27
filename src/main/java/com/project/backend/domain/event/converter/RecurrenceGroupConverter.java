package com.project.backend.domain.event.converter;

import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.member.entity.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecurrenceGroupConverter {

    public static RecurrenceGroup toRecurrenceGroup(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time, Member member) {
        return RecurrenceGroup.builder()
                .frequency(req.frequency())
                .intervalValue(req.getIntervalOrDefault()) // 디폴트 값 설정
                .daysOfWeek(resolveWeeklyDays(req, time))
                .monthlyType(req.monthlyType())
                .daysOfMonth(resolveMonthlyDays(req, time))
                .weekOfMonth(resolveMonthlyWeek(req, time))
                .dayOfWeekInMonth(resolveMonthlyWeekDays(req, time))
                .monthOfYear(resolveYearlyMonth(req, time))
                .isCustom(isCustomRecurrence(req))
                .endType(req.endType())
                .endDate(req.endDate())
                .occurrenceCount(req.occurrenceCount())
                .createdCount(1)
                .member(member)
                .build();
    }

    private static String resolveWeeklyDays(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time) {
        if (req.frequency() != RecurrenceFrequency.WEEKLY) {
            return null;
        }

        if (req.daysOfWeek() == null || req.daysOfWeek().isEmpty()) {
            return time.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    .toUpperCase();
        }

        return req.daysOfWeek().stream()
                .distinct()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private static String resolveMonthlyWeekDays(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time) {
        if (req.frequency() != RecurrenceFrequency.MONTHLY) {
            return null;
        }

        if (req.monthlyType() != MonthlyType.DAY_OF_WEEK) {
            return null;
        }

        if (req.dayOfWeekInMonth() == null || req.dayOfWeekInMonth().isEmpty()) {
            return time.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    .toUpperCase();
        }

        return req.dayOfWeekInMonth().stream()
                .distinct()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private static Integer resolveMonthlyWeek(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time) {
        if (req.frequency() != RecurrenceFrequency.MONTHLY) {
            return null;
        }

        if (req.monthlyType() != MonthlyType.DAY_OF_WEEK) {
            return null;
        }

        if (req.weekOfMonth() == null) {
            return (time.getDayOfMonth() - 1) / 7 + 1;
        }

        return req.weekOfMonth();
    }

    private static String resolveMonthlyDays(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time) {
        if (req.frequency() == RecurrenceFrequency.WEEKLY) {
            return null;
        }

        if (req.monthlyType() == MonthlyType.DAY_OF_WEEK) {
            return null;
        }

        if (req.daysOfMonth() == null || req.daysOfMonth().isEmpty()) {
            return String.valueOf(time.getDayOfMonth());
        }

        return req.daysOfMonth().stream()
                .distinct()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private static Integer resolveYearlyMonth(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time) {
        if (req.frequency() != RecurrenceFrequency.YEARLY) {
            return null;
        }

        if (req.monthOfYear() == null) {
            return time.getMonthValue();
        }

        return req.monthOfYear();
    }

    private static Boolean isCustomRecurrence(RecurrenceGroupReqDTO.CreateReq req) {
        return req.daysOfWeek() != null
                || req.monthlyType() != null
                || req.daysOfMonth() != null
                || req.weekOfMonth() != null
                || req.dayOfWeekInMonth() != null
                || req.monthOfYear() != null;
    }

}
