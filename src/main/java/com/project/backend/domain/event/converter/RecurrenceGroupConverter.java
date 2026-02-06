package com.project.backend.domain.event.converter;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.dto.response.RecurrenceGroupResDTO;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.*;
import com.project.backend.domain.member.entity.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
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
                .monthlyWeekdayRule(rgSpec.weekdayRule())
                .dayOfWeekInMonth(resolveMonthlyWeekDays(rgSpec))
                .monthOfYear(resolveYearlyMonth(rgSpec))
                .isCustom(isCustomRecurrence(rgSpec))
                .endType(rgSpec.endType())
                .endDate(rgSpec.endDate())
                .occurrenceCount(rgSpec.occurrenceCount())
                .createdCount(1)
                .member(member)
                .event(null)
                .build();
    }

    public static RecurrenceGroupSpec from(
            RecurrenceGroupReqDTO.CreateReq req,
            LocalDateTime startTime
    ) {
        RecurrenceFrequency frequency = req.frequency();

        RecurrenceGroupSpec.RecurrenceGroupSpecBuilder b =
                RecurrenceGroupSpec.builder()
                        .frequency(frequency)
                        .interval(req.intervalValue() != null ? req.intervalValue() : 1)
                        .endType(req.endType() != null ? req.endType() : RecurrenceEndType.NEVER)
                        .endDate(req.endDate())
                        .occurrenceCount(req.occurrenceCount());

        switch (frequency) {

            case DAILY -> {
                // DAILY는 추가 필드 없음
            }

            case WEEKLY -> {
                // daysOfWeek 없으면 startTime 기준 요일 자동 설정
                b.daysOfWeek(
                        req.daysOfWeek() != null && !req.daysOfWeek().isEmpty()
                                ? req.daysOfWeek()
                                : List.of(startTime.getDayOfWeek())
                );
            }

            case MONTHLY -> {
                MonthlyType monthlyType =
                        req.monthlyType() != null
                                ? req.monthlyType()
                                : MonthlyType.DAY_OF_MONTH; // 디폴트

                b.monthlyType(monthlyType);

                if (monthlyType == MonthlyType.DAY_OF_MONTH) {
                    b.daysOfMonth(
                            req.daysOfMonth() != null && !req.daysOfMonth().isEmpty()
                                    ? req.daysOfMonth()
                                    : List.of(startTime.getDayOfMonth())
                    );
                }

                if (monthlyType == MonthlyType.DAY_OF_WEEK) {
                    b.weekOfMonth(
                            req.weekOfMonth() != null
                                    ? req.weekOfMonth()
                                    : getWeekOfMonth(startTime)
                    );

                    MonthlyWeekdayRule rule =
                            req.weekdayRule() != null
                                    ? req.weekdayRule()
                                    : MonthlyWeekdayRule.SINGLE;

                    b.weekdayRule(rule);

                    List<DayOfWeek> daysOfWeekInMonth =
                            rule == MonthlyWeekdayRule.SINGLE
                                    ? req.dayOfWeekInMonth()
                                    : null;

                    b.dayOfWeekInMonth(daysOfWeekInMonth);
                }
            }

            case YEARLY -> {
                b.monthOfYear(
                        req.monthOfYear() != null
                                ? req.monthOfYear()
                                : startTime.getMonthValue()
                );

                b.daysOfMonth(
                        req.daysOfMonth() != null && !req.daysOfMonth().isEmpty()
                                ? req.daysOfMonth()
                                : List.of(startTime.getDayOfMonth())
                );
            }

            default -> throw new IllegalArgumentException("Unsupported frequency");
        }

        return b.build();
    }


    public static RecurrenceGroupSpec from(
            RecurrenceGroupReqDTO.UpdateReq req,
            RecurrenceGroup rg,
            LocalDateTime time) {
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
                .weekdayRule(recurrenceGroup.getMonthlyWeekdayRule())
                .dayOfWeekInMonth(getDayOfWeeksInMonth(recurrenceGroup.getDayOfWeekInMonth()))
                .monthOfYear(recurrenceGroup.getMonthOfYear())
                .endType(recurrenceGroup.getEndType())
                .occurrenceCount(recurrenceGroup.getOccurrenceCount())
                .endDate(recurrenceGroup.getEndDate())
                .build();
    }

    public static RecurrenceException toRecurrenceExceptionForDelete(
            RecurrenceGroup recurrenceGroup,
            LocalDateTime time) {
        return RecurrenceException.builder()
                .exceptionDate(time.toLocalDate())
                .title(null)
                .content(null)
                .startTime(null)
                .endTime(null)
                .exceptionType(ExceptionType.SKIP)
                .location(null)
                .color(null)
                .isAllDay(null)
                .recurrenceGroup(recurrenceGroup)
                .build();
    }

    public static RecurrenceException toRecurrenceExceptionForUpdate(
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

        return serializeDaysOfWeek(rgSpec.daysOfWeek());
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
            LocalDateTime startTime
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
                List<DayOfWeek> days =
                        req.daysOfWeek() != null
                                ? req.daysOfWeek()
                                : (rg.getDaysOfWeekAsList() != null && !rg.getDaysOfWeekAsList().isEmpty()
                                ? toDayOfWeekList(rg.getDaysOfWeek())
                                : List.of(startTime.getDayOfWeek()));

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
                                    : List.of(startTime.getDayOfMonth()));

                    b.daysOfMonth(daysOfMonth);
                }

                if (monthlyType == MonthlyType.DAY_OF_WEEK) {
                    Integer weekOfMonth =
                            req.weekOfMonth() != null
                                    ? req.weekOfMonth()
                                    : (rg.getWeekOfMonth() != null ? rg.getWeekOfMonth()
                                    : getWeekOfMonth(startTime));

                    b.weekOfMonth(weekOfMonth);

                    MonthlyWeekdayRule rule =
                            req.weekdayRule() != null ? req.weekdayRule() : rg.getMonthlyWeekdayRule();

                    List<DayOfWeek> daysOfWeekInMonth = resolveDaysFromRule(
                            rule,
                            req.dayOfWeekInMonth(),
                            toDayOfWeekList(rg.getDayOfWeekInMonth()),
                            startTime
                    );

                    b.dayOfWeekInMonth(daysOfWeekInMonth);
                }
            }

            case YEARLY -> {
                Integer monthOfYear =
                        req.monthOfYear() != null
                                ? req.monthOfYear()
                                : (rg.getMonthOfYear() != null
                                ? rg.getMonthOfYear()
                                : startTime.getMonthValue());

                b.monthOfYear(monthOfYear);
            }
        }
    }

    private static List<DayOfWeek> resolveDaysFromRule(
            MonthlyWeekdayRule rule,
            List<DayOfWeek> rawDays,
            List<DayOfWeek> fallbackDays,
            LocalDateTime baseTime
    ) {
        return switch (rule) {

            case SINGLE -> {
                // 수정 요청에 명시적으로 요일이 온 경우
                if (rawDays != null && !rawDays.isEmpty()) {
                    yield rawDays;
                }

                // 기존 반복 그룹에 저장된 요일
                if (fallbackDays != null && !fallbackDays.isEmpty()) {
                    yield fallbackDays;
                }

                // 최후 보루: startTime 요일
                yield List.of(baseTime.getDayOfWeek());
            }

            case WEEKDAY, WEEKEND, ALL_DAYS -> null;
        };
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

    private static String serializeDaysOfWeek(List<DayOfWeek> daysOfWeek) {
        return daysOfWeek.stream()
                .distinct()
                .sorted()
                .map(DayOfWeek::name)
                .collect(Collectors.joining(","));
    }

    private static List<DayOfWeek> toDayOfWeekList(String daysOfWeek) {
        return Arrays.stream(daysOfWeek.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .toList();
    }
}
