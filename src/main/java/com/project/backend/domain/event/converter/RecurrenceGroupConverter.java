package com.project.backend.domain.event.converter;

import com.project.backend.domain.common.plan.enums.MonthlyWeekdayRule;
import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.dto.response.RecurrenceGroupResDTO;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.*;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.global.recurrence.util.RecurrenceUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
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

        normalizeFrequencyForCreate(req, startTime, frequency, b);

        return b.build();
    }

    public static RecurrenceGroupSpec from(
            EventReqDTO.UpdateReq eventReq,
            RecurrenceGroupReqDTO.UpdateReq rgReq,
            RecurrenceGroup rg,
            LocalDateTime time) {
        RecurrenceGroupSpec.RecurrenceGroupSpecBuilder b = RecurrenceGroupSpec.builder();

        // 반복그룹에 대한 변경이 없고, startTime, endTime을 제외한 일정 필드만을 변경한 경우
        if (rgReq == null && eventReq.startTime() == null && eventReq.endTime() == null) {
            keepExistingRecurrence(rg, b);
        } else {
            normalizeFrequencyForUpdate(b, rgReq, rg, time);
            normalizeEndCondition(b, rgReq, rg);
        }
        return b.build();
    }


    public static RecurrenceGroupResDTO.DetailRes toDetailRes(RecurrenceGroup recurrenceGroup) {
        if (recurrenceGroup == null) {
            return null;
        }

        // "MONDAY,WEDNESDAY" → List<DayOfWeek>
        List<DayOfWeek> daysOfWeek = recurrenceGroup.getDaysOfWeek() != null
                ? RecurrenceUtils.parseDaysOfWeek(recurrenceGroup.getDaysOfWeek())
                : null;

        // "1,15" → List<Integer>
        List<Integer> daysOfMonth = recurrenceGroup.getDaysOfMonth() != null
                ? RecurrenceUtils.parseDaysOfMonth(recurrenceGroup.getDaysOfMonth())
                : null;

        // "MONDAY" → DayOfWeek
        List<DayOfWeek> dayOfWeekInMonth = recurrenceGroup.getDayOfWeekInMonth() != null
                ? RecurrenceUtils.parseDaysOfWeek(recurrenceGroup.getDayOfWeekInMonth())
                : null;

        // "MONDAY,WEDNESDAY" → List<DayOfWeek>
        MonthlyWeekdayRule weekdayRule = RecurrenceUtils.inferWeekdayRule(dayOfWeekInMonth);


        return RecurrenceGroupResDTO.DetailRes.builder()
                .id(recurrenceGroup.getId())
                .frequency(recurrenceGroup.getFrequency())
                .isCustom(recurrenceGroup.getIsCustom())
                .interval(recurrenceGroup.getIntervalValue())
                .daysOfWeek(daysOfWeek)
                .monthlyType(recurrenceGroup.getMonthlyType())
                .daysOfMonth(daysOfMonth)
                .weekOfMonth(recurrenceGroup.getWeekOfMonth())
                .weekdayRule(weekdayRule)
                .dayOfWeekInMonth(dayOfWeekInMonth)
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
                .exceptionDate(time)
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
            LocalDateTime occurrenceDate
    ) {
        return RecurrenceException.builder()
                .exceptionDate(occurrenceDate)
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

    private static void normalizeFrequencyForCreate(
            RecurrenceGroupReqDTO.CreateReq req,
            LocalDateTime startTime,
            RecurrenceFrequency frequency,
            RecurrenceGroupSpec.RecurrenceGroupSpecBuilder b) {
        switch (frequency) {

            case DAILY -> {
                // DAILY는 추가 필드 없음
            }

            case WEEKLY -> {
                List<DayOfWeek> daysOfWeek = req.daysOfWeek();

                if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                    daysOfWeek = List.of(startTime.getDayOfWeek());
                }

                daysOfWeek = daysOfWeek.stream()
                        .distinct()
                        .sorted()
                        .toList();

                b.daysOfWeek(daysOfWeek);
            }

            case MONTHLY -> {
                MonthlyType monthlyType =
                        req.monthlyType() != null
                                ? req.monthlyType()
                                : MonthlyType.DAY_OF_MONTH; // 디폴트

                b.monthlyType(monthlyType);

                if (monthlyType == MonthlyType.DAY_OF_MONTH) {
                    List<Integer> daysOfMonth = req.daysOfMonth();

                    if (daysOfMonth == null || daysOfMonth.isEmpty()) {
                        daysOfMonth = List.of(startTime.getDayOfMonth());
                    }

                    daysOfMonth = daysOfMonth.stream()
                            .distinct()
                            .sorted()
                            .toList();

                    b.daysOfMonth(daysOfMonth);
                }

                if (monthlyType == MonthlyType.DAY_OF_WEEK) {
                    Integer weekOfMonth = req.weekOfMonth() != null ? req.weekOfMonth() : getWeekOfMonth(startTime);

                    b.weekOfMonth(weekOfMonth);

                    MonthlyWeekdayRule rule =
                            req.weekdayRule() != null
                                    ? req.weekdayRule()
                                    : MonthlyWeekdayRule.SINGLE;

                    List<DayOfWeek> daysOfWeekInMonth =
                            rule == MonthlyWeekdayRule.SINGLE
                            ? (req.dayOfWeekInMonth() != null
                                    ? List.of(req.dayOfWeekInMonth())
                                    : List.of(startTime.getDayOfWeek())) : resolveDaysFromRule(rule);

                    b.dayOfWeekInMonth(daysOfWeekInMonth);
                }
            }

            case YEARLY ->  {
                Integer monthOfYear = req.monthOfYear() != null ? req.monthOfYear() : startTime.getMonthValue();
                b.monthOfYear(monthOfYear);
            }

            default -> throw new IllegalArgumentException("Unsupported frequency");
        }
    }

    private static void normalizeFrequencyForUpdate(
            RecurrenceGroupSpec.RecurrenceGroupSpecBuilder b,
            RecurrenceGroupReqDTO.UpdateReq req,
            RecurrenceGroup rg,
            LocalDateTime startTime // EventReqDTO.UpdateReq의 startTime or occurrenceDate (update로직 초반에 계산)
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
                List<DayOfWeek> days = req.daysOfWeek();

                // daysOfWeek 없으면 startTime 기준 요일 자동 설정
                if (days == null || days.isEmpty()) {
                    days = List.of(startTime.getDayOfWeek());
                }

                // 정규화(중복 제거 + 정렬)
                days = days.stream()
                        .distinct()
                        .sorted()
                        .toList();

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
                    List<Integer> daysOfMonth = req.daysOfMonth();

                    if (daysOfMonth == null || daysOfMonth.isEmpty()) {
                        daysOfMonth = List.of(startTime.getDayOfMonth());
                    }

                    daysOfMonth = daysOfMonth.stream()
                            .distinct()
                            .sorted()
                            .toList();

                    b.daysOfMonth(daysOfMonth);
                }

                if (monthlyType == MonthlyType.DAY_OF_WEEK) {
                    Integer weekOfMonth = req.weekOfMonth() != null ? req.weekOfMonth() : getWeekOfMonth(startTime);

                    b.weekOfMonth(weekOfMonth);

                    MonthlyWeekdayRule rule =
                            req.weekdayRule() != null
                                    ? req.weekdayRule()
                                    : (rg != null
                                    && RecurrenceUtils.parseDaysOfWeek(rg.getDayOfWeekInMonth()).size() > 1
                                    ? RecurrenceUtils.inferWeekdayRule(
                                            RecurrenceUtils.parseDaysOfWeek(rg.getDayOfWeekInMonth()))
                                    : MonthlyWeekdayRule.SINGLE);
                    List<DayOfWeek> daysOfWeekInMonth =
                            rule == MonthlyWeekdayRule.SINGLE
                                    ? (req.dayOfWeekInMonth() != null
                                    ? List.of(req.dayOfWeekInMonth())
                                    : (rg != null
                                    && RecurrenceUtils.parseDaysOfWeek(rg.getDayOfWeekInMonth()).size() == 1
                                    ? RecurrenceUtils.parseDaysOfWeek(rg.getDayOfWeekInMonth())
                                    : List.of(startTime.getDayOfWeek())))
                                    : resolveDaysFromRule(rule);

                    b.dayOfWeekInMonth(daysOfWeekInMonth);
                }
            }

            case YEARLY -> {
                Integer monthOfYear = req.monthOfYear() != null ? req.monthOfYear() : startTime.getMonthValue();

                b.monthOfYear(monthOfYear);
            }
        }
    }

    private static List<DayOfWeek> resolveDaysFromRule(MonthlyWeekdayRule rule) {
        return switch (rule) {
            case WEEKDAY -> List.of(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
            );
            case WEEKEND -> List.of(
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
            );

            case ALL_DAYS -> List.of(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
            );
            case SINGLE -> throw new IllegalArgumentException("SINGLE rule should not be used here");
        };
    }
    private static void normalizeEndCondition(
            RecurrenceGroupSpec.RecurrenceGroupSpecBuilder b,
            RecurrenceGroupReqDTO.UpdateReq req,
            RecurrenceGroup rg) {
        RecurrenceEndType endType =
                req.endType() != null ? req.endType() :
                        rg != null ? rg.getEndType() : RecurrenceEndType.NEVER;

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

    private static void keepExistingRecurrence(RecurrenceGroup rg, RecurrenceGroupSpec.RecurrenceGroupSpecBuilder b) {
        b.frequency(rg.getFrequency());
        b.interval(rg.getIntervalValue());
        b.daysOfWeek(rg.getDaysOfWeek() != null ? RecurrenceUtils.parseDaysOfWeek(rg.getDaysOfWeek()) : null);
        b.monthlyType(rg.getMonthlyType());
        b.daysOfMonth(rg.getDaysOfMonthAsList());
        b.weekOfMonth(rg.getWeekOfMonth());
        b.dayOfWeekInMonth(rg.getDayOfWeekInMonth() != null
                ? RecurrenceUtils.parseDaysOfWeek(rg.getDayOfWeekInMonth()) : null);
        b.monthOfYear(rg.getMonthOfYear());
        b.endType(rg.getEndType());
        b.endDate(rg.getEndDate());
        b.occurrenceCount(rg.getOccurrenceCount());
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
}
