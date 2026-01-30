package com.project.backend.domain.event.converter;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.dto.response.RecurrenceGroupResDTO;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.ExceptionType;
import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.member.entity.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecurrenceGroupConverter {

    public static RecurrenceGroup toRecurrenceGroup(RecurrenceGroupSpec rgSpec, Member member) {
        return RecurrenceGroup.builder()
                .frequency(rgSpec.frequency())
                .intervalValue(rgSpec.interval())
                .daysOfWeek(resolveWeeklyDays(rgSpec))
                .monthlyType(rgSpec.monthlyType())
                .daysOfMonth(resolveMonthlyDays(rgSpec))
                .weekOfMonth(resolveMonthlyWeek(rgSpec))
                .dayOfWeekInMonth(resolveMonthlyWeekDays(rgSpec))
                .monthOfYear(resolveYearlyMonth(rgSpec))
                .isCustom(isCustomRecurrence(rgSpec))
                .endType(rgSpec.endType())
                .endDate(rgSpec.endDate())
                .occurrenceCount(rgSpec.occurrenceCount())
                .createdCount(1)
                .member(member)
                .build();
    }

    public static RecurrenceGroupSpec from(RecurrenceGroupReqDTO.CreateReq req) {
        return RecurrenceGroupSpec.builder()
                .frequency(req.frequency())
                .interval(req.intervalValue() != null ? req.intervalValue() : 1)
                .daysOfWeek(req.daysOfWeek())
                .monthlyType(req.monthlyType())
                .daysOfMonth(req.daysOfMonth())
                .weekOfMonth(req.weekOfMonth())
                .dayOfWeekInMonth(req.dayOfWeekInMonth())
                .monthOfYear(req.monthOfYear())
                .endType(req.endType() != null ? req.endType() : RecurrenceEndType.NEVER)
                .endDate(req.endDate())
                .occurrenceCount(req.occurrenceCount())
                .build();
    }

    public static RecurrenceGroupSpec from(RecurrenceGroupReqDTO.UpdateReq req, RecurrenceGroup rg, LocalDateTime time) {
        RecurrenceGroupSpec.RecurrenceGroupSpecBuilder b = RecurrenceGroupSpec.builder();

        normalizeFrequency(b, req, rg, time);
        normalizeEndCondition(b, req, rg);

        return b.build();
    }


    public static RecurrenceGroupResDTO.DetailRes toDetailRes(RecurrenceGroup recurrenceGroup) {
        if (recurrenceGroup == null) {
            return null;
        }
        return RecurrenceGroupResDTO.DetailRes.builder()
                .id(recurrenceGroup.getId())
                .frequency(recurrenceGroup.getFrequency())
                .isCustom(recurrenceGroup.getIsCustom())
                .interval(recurrenceGroup.getIntervalValue())
                .daysOfWeek(getDayOfWeeks(recurrenceGroup.getDaysOfWeek()))
                .monthlyType(recurrenceGroup.getMonthlyType())
                .daysOfMonth(getDaysOfMonth(recurrenceGroup.getDaysOfMonth()))
                .weekOfMonth(recurrenceGroup.getWeekOfMonth())
                .dayOfWeekInMonth(getDayOfWeeksInMonth(recurrenceGroup.getDayOfWeekInMonth()))
                .monthOfYear(recurrenceGroup.getMonthOfYear())
                .endType(recurrenceGroup.getEndType())
                .occurrenceCount(recurrenceGroup.getOccurrenceCount())
                .endDate(recurrenceGroup.getEndDate())
                .build();
    }

    public static RecurrenceException toRecurrenceException(
            EventReqDTO.UpdateReq req,
            RecurrenceGroup recurrenceGroup,
            LocalDateTime time) {
        return RecurrenceException.builder()
                .exceptionDate(time.toLocalDate())
                .title(req.title() != null ? req.title() : null)
                .content(req.content() != null ? req.content() : null)
                .startTime(req.startTime() != null ? req.startTime() : null)
                .endTime(req.endTime() != null ? req.endTime() : null)
                .exceptionType(ExceptionType.OVERRIDE)
                .location(req.location() != null ? req.location() : null)
                .color(req.color() != null ? req.color() : null)
                .isAllDay(req.isAllDay() != null ? req.isAllDay() : null)
                .recurrenceGroup(recurrenceGroup)
                .build();
    }

    public static RecurrenceGroupReqDTO.CreateReq toCreateReq(RecurrenceGroupReqDTO.UpdateReq req) {
        return RecurrenceGroupReqDTO.CreateReq.builder()
                .frequency(req.frequency())
                .intervalValue(req.intervalValue())
                .daysOfWeek(req.daysOfWeek())
                .monthlyType(req.monthlyType())
                .daysOfMonth(req.daysOfMonth())
                .weekOfMonth(req.weekOfMonth())
                .dayOfWeekInMonth(req.dayOfWeekInMonth())
                .monthOfYear(req.monthOfYear())
                .endType(req.endType())
                .endDate(req.endDate())
                .occurrenceCount(req.occurrenceCount())
                .build();
    }

    private static String resolveWeeklyDays(RecurrenceGroupSpec rgSpec) {
        if (rgSpec.frequency() != RecurrenceFrequency.WEEKLY) {
            return null;
        }

        // null or empty → DB에 저장하지 않음
        if (rgSpec.daysOfWeek() == null || rgSpec.daysOfWeek().isEmpty()) {
            return null;
        }

        return rgSpec.daysOfWeek().stream()
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static String resolveMonthlyWeekDays(RecurrenceGroupSpec rgSpec) {
        if (rgSpec.frequency() != RecurrenceFrequency.MONTHLY) {
            return null;
        }

        if (rgSpec.monthlyType() != MonthlyType.DAY_OF_WEEK) {
            return null;
        }

        if (rgSpec.dayOfWeekInMonth() == null || rgSpec.dayOfWeekInMonth().isEmpty()) {
            return null;
        }

        return rgSpec.dayOfWeekInMonth().stream()
                .distinct()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private static Integer resolveMonthlyWeek(RecurrenceGroupSpec rgSpec) {
        if (rgSpec.frequency() != RecurrenceFrequency.MONTHLY) {
            return null;
        }

        if (rgSpec.monthlyType() != MonthlyType.DAY_OF_WEEK) {
            return null;
        }

        return rgSpec.weekOfMonth();
    }

    private static String resolveMonthlyDays(RecurrenceGroupSpec rgSpec) {
        if (rgSpec.frequency() == RecurrenceFrequency.WEEKLY) {
            return null;
        }

        if (rgSpec.monthlyType() != MonthlyType.DAY_OF_MONTH) {
            return null;
        }

        if (rgSpec.daysOfMonth() == null || rgSpec.daysOfMonth().isEmpty()){
            return null;
        }

        return rgSpec.daysOfMonth().stream()
                .distinct()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private static Integer resolveYearlyMonth(RecurrenceGroupSpec rgSpec) {
        if (rgSpec.frequency() != RecurrenceFrequency.YEARLY) {
            return null;
        }

        return rgSpec.monthOfYear();
    }

    private static Boolean isCustomRecurrence(RecurrenceGroupSpec rgSpec) {
        return (rgSpec.daysOfWeek() != null && !rgSpec.daysOfWeek().isEmpty())
                || rgSpec.monthlyType() != null
                || (rgSpec.daysOfMonth() != null && !rgSpec.daysOfMonth().isEmpty())
                || rgSpec.weekOfMonth() != null
                || (rgSpec.dayOfWeekInMonth() != null && !rgSpec.dayOfWeekInMonth().isEmpty())
                || rgSpec.monthOfYear() != null;
    }

    private static List<String> getDayOfWeeks(String daysOfWeek) {
        if (daysOfWeek == null) {
            return List.of();
        }
        return List.of(daysOfWeek.split(","));
    }

    private static List<Integer> getDaysOfMonth(String daysOfMonth) {
        if (daysOfMonth == null) {
            return List.of();
        }
        return Stream.of(daysOfMonth.split(","))
                .map(Integer::parseInt)
                .toList();
    }

    private static List<String> getDayOfWeeksInMonth(String dayOfWeekInMonth) {
        if (dayOfWeekInMonth == null) {
            return List.of();
        }
        return List.of(dayOfWeekInMonth.split(","));
    }

    private static void normalizeFrequency(
            RecurrenceGroupSpec.RecurrenceGroupSpecBuilder b,
            RecurrenceGroupReqDTO.UpdateReq req,
            RecurrenceGroup rg,
            LocalDateTime time
    ) {
        RecurrenceFrequency frequency =
                req.frequency() != null ? req.frequency() : rg.getFrequency();

        b.frequency(frequency);

        Integer interval;
        if (frequency == RecurrenceFrequency.WEEKLY) {
            // WEEKLY는 무조건 1
            interval = 1;
        } else {
            // WEEKLY가 아닌 경우만 req → rg 순으로 선택
            interval = req.intervalValue() != null ? req.intervalValue() : rg.getIntervalValue();
        }

        b.interval(interval);

        // 전부 초기화
        b.daysOfWeek(null);
        b.monthlyType(null);
        b.daysOfMonth(null);
        b.weekOfMonth(null);
        b.dayOfWeekInMonth(null);
        b.monthOfYear(null);

        switch (frequency) {
            case WEEKLY -> {
                // daysOfWeek 없으면 startTime 기준 요일 자동 설정
                List<String> days =
                        req.daysOfWeek() != null
                                ? req.daysOfWeek()
                                : (rg.getDaysOfWeekAsList() != null && !rg.getDaysOfWeekAsList().isEmpty()
                                ? rg.getDaysOfWeekAsList()
                                : List.of(time.getDayOfWeek().name().substring(0, 3)));

                b.daysOfWeek(days);
            }

            case MONTHLY -> {
                // monthlyType 기본값: DAY_OF_MONTH
                MonthlyType monthlyType =
                        req.monthlyType() != null
                                ? req.monthlyType()
                                : (rg.getMonthlyType() != null
                                ? rg.getMonthlyType()
                                : MonthlyType.DAY_OF_MONTH);

                b.monthlyType(monthlyType);

                if (monthlyType == MonthlyType.DAY_OF_MONTH) {
                    List<Integer> daysOfMonth =
                            req.daysOfMonth() != null
                                    ? req.daysOfMonth()
                                    : (rg.getDaysOfMonthAsList() != null && !rg.getDaysOfMonthAsList().isEmpty()
                                    ? rg.getDaysOfMonthAsList()
                                    : List.of(time.getDayOfMonth()));

                    b.daysOfMonth(daysOfMonth);
                }

                if (monthlyType == MonthlyType.DAY_OF_WEEK) {
                    Integer weekOfMonth =
                            req.weekOfMonth() != null
                                    ? req.weekOfMonth()
                                    : (rg.getWeekOfMonth() != null ? rg.getWeekOfMonth()
                                    : getWeekOfMonth(time));

                    List<String> dayOfWeekInMonth =
                            req.dayOfWeekInMonth() != null
                                    ? req.dayOfWeekInMonth()
                                    : (rg.getDayOfWeekInMonthAsList() != null
                                    && !rg.getDayOfWeekInMonthAsList().isEmpty()
                                    ? rg.getDayOfWeekInMonthAsList()
                                    : List.of(time.getDayOfWeek().name().substring(0, 3)));

                    b.weekOfMonth(weekOfMonth);
                    b.dayOfWeekInMonth(dayOfWeekInMonth);
                }
            }

            case YEARLY -> {
                Integer monthOfYear =
                        req.monthOfYear() != null
                                ? req.monthOfYear()
                                : (rg.getMonthOfYear() != null
                                ? rg.getMonthOfYear()
                                : time.getMonthValue());

                b.monthOfYear(monthOfYear);
            }
        }
    }

    private static void normalizeEndCondition(
            RecurrenceGroupSpec.RecurrenceGroupSpecBuilder b,
            RecurrenceGroupReqDTO.UpdateReq req,
            RecurrenceGroup rg) {
        RecurrenceEndType endType =
                req.endType() != null ? req.endType() : rg.getEndType();

        b.endType(endType);

        switch (endType) {
            case NEVER -> {
                b.endDate(null);
                b.occurrenceCount(null);
            }
            case END_BY_DATE -> {
                b.endDate(req.endDate() != null ? req.endDate() : rg.getEndDate());
                b.occurrenceCount(null);
            }
            case END_BY_COUNT -> {
                b.occurrenceCount(req.occurrenceCount() != null
                        ? req.occurrenceCount()
                        : rg.getOccurrenceCount());
                b.endDate(null);
            }
        }
    }
    private static int getWeekOfMonth(LocalDateTime time) {
        return (time.getDayOfMonth() - 1) / 7 + 1;
    }
}
