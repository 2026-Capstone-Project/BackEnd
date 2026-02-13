package com.project.backend.domain.event.validator;

import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.MonthlyWeekdayRule;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.event.exception.RecurrenceGroupErrorCode;
import com.project.backend.domain.event.exception.RecurrenceGroupException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

// TODO static으로 바꾸기
@Slf4j
@Component
public class RecurrenceGroupValidator {

    // 생성 전용
    public void validateCreate(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time) {
        validateEndTypeForCreate(req, time);
        validateFrequencyRuleForCreate(req);
        validateWeeklyDays(req);
        validateMonthlyDayOfWeek(req);
    }

    // 수정 전용
    // 단일 일정 or 반복이 포함된 일정 수정 시
    public void validateUpdate(RecurrenceGroupReqDTO.UpdateReq req, RecurrenceGroup baseRg, LocalDateTime time) {
        validateEndTypeForUpdate(req, baseRg, time);
        validateFrequencyRuleForUpdate(req, baseRg);
        validateWeeklyDaysForUpdate(req, baseRg);
        validateMonthlyDayOfWeekForUpdate(req, baseRg);
    }

    public void validateEndTypeForCreate(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time) {
        RecurrenceEndType endType =
                req.endType() != null ? req.endType() : RecurrenceEndType.NEVER;

        switch (endType) {
            case NEVER -> {
                if (req.endDate() != null || req.occurrenceCount() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
            }
            case END_BY_DATE -> {
                if (req.occurrenceCount() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
                if (req.endDate() == null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.END_DATE_REQUIRED);
                }
                if (req.endDate().isBefore(time.toLocalDate())) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_DATE_RANGE);
                }
            }
            case END_BY_COUNT -> {
                if (req.endDate() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
                if (req.occurrenceCount() == null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.END_COUNT_REQUIRED);
                }
            }
            default -> throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_TYPE);
        }
    }

    public void validateEndTypeForUpdate(RecurrenceGroupReqDTO.UpdateReq req, RecurrenceGroup rg, LocalDateTime time) {
        RecurrenceEndType endType
                = req.endType() != null ? req.endType() : rg.getEndType();

        LocalDate finalEndDate =
                req.endDate() != null ? req.endDate()
                        : (endType == RecurrenceEndType.END_BY_DATE ? rg.getEndDate() : null);

        Integer finalCount =
                req.occurrenceCount() != null ? req.occurrenceCount()
                        : (endType == RecurrenceEndType.END_BY_COUNT ? rg.getOccurrenceCount() : null);


        switch (endType) {
            case NEVER -> {
                if (finalEndDate != null || finalCount != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
            }
            case END_BY_DATE -> {
                if (finalCount != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
                if (finalEndDate == null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.END_DATE_REQUIRED);
                }
                if (finalEndDate.isBefore(time.toLocalDate())) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_DATE_RANGE);
                }
            }
            case END_BY_COUNT -> {
                if (finalEndDate != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
                if (finalCount == null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.END_COUNT_REQUIRED);
                }
            }
        }
    }

    public void validateFrequencyRuleForCreate(RecurrenceGroupReqDTO.CreateReq req) {
        if (req.frequency() == null) {
            throw new RecurrenceGroupException(RecurrenceGroupErrorCode.RECURRENCE_FREQUENCY_REQUIRED);
        }

        switch (req.frequency()) {
            case DAILY -> {
                if (req.daysOfWeek() != null || req.daysOfMonth() != null
                        || req.weekOfMonth() != null || req.dayOfWeekInMonth() != null || req.monthlyType() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                // 값이 범위를 벗어난 경우 (0 이하 또는 365 이상)
                if (req.intervalValue() != null && (req.intervalValue() <= 0 || req.intervalValue() >= 365)) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAILY_INTERVAL_VALUE);
                }
            }
            case WEEKLY -> {
                if (req.daysOfMonth() != null || req.weekOfMonth() != null
                        || req.dayOfWeekInMonth() != null || req.monthlyType() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                if (req.intervalValue() != null && req.intervalValue() != 1) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_WEEKLY_INTERVAL_VALUE);
                }
            }
            case MONTHLY -> {
                if ( req.daysOfWeek() != null || req.monthOfYear() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                if (req.intervalValue() !=null && (req.intervalValue() <= 0 || req.intervalValue() >= 12)) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_MONTHLY_INTERVAL_VALUE);
                }
                if (req.monthlyType() == MonthlyType.DAY_OF_MONTH
                        && (req.weekOfMonth() != null || req.dayOfWeekInMonth() != null || req.weekdayRule() != null)) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                if (req.weekdayRule() == MonthlyWeekdayRule.SINGLE) {
                    if (req.dayOfWeekInMonth() != null && req.dayOfWeekInMonth().size() != 1) {
                        throw new RecurrenceGroupException(
                                RecurrenceGroupErrorCode.INVALID_SIZE_OF_DAY_OF_WEEK_IN_MONTH
                        );
                    }
                } else {
                    if (req.weekdayRule() != null && req.dayOfWeekInMonth() != null) {
                        throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAY_OF_WEEK_IN_MONTH);
                    }
                }
            }
            case YEARLY -> {
                if (req.daysOfWeek() != null || req.weekOfMonth() != null || req.daysOfMonth() != null
                        || req.dayOfWeekInMonth() != null || req.monthlyType() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                if (req.intervalValue() != null && (req.intervalValue() <= 0 || req.intervalValue() >= 100)) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_YEARLY_INTERVAL_VALUE);
                }
            }
        }
    }

    public void validateFrequencyRuleForUpdate(RecurrenceGroupReqDTO.UpdateReq req, RecurrenceGroup baseRg) {
        RecurrenceFrequency frequency =
                req.frequency() != null ? req.frequency() : baseRg.getFrequency();

        List<DayOfWeek> daysOfWeek =
                req.daysOfWeek() != null ? req.daysOfWeek()
                        : (frequency == RecurrenceFrequency.WEEKLY ? toDayOfWeekList(baseRg.getDaysOfWeek()) : null);

        MonthlyType monthlyType =
                req.monthlyType() != null ? req.monthlyType()
                        : (frequency == RecurrenceFrequency.MONTHLY ? baseRg.getMonthlyType() : null);
        Integer weekOfMonth =
                req.weekOfMonth() != null ? req.weekOfMonth()
                        : (frequency == RecurrenceFrequency.MONTHLY
                        && monthlyType == MonthlyType.DAY_OF_WEEK
                        ? baseRg.getWeekOfMonth() : null);

        List<DayOfWeek> dayOfWeekInMonth =
                req.dayOfWeekInMonth() != null ? req.dayOfWeekInMonth()
                        : (frequency == RecurrenceFrequency.MONTHLY
                        && monthlyType == MonthlyType.DAY_OF_WEEK
                        ? toDayOfWeekList(baseRg.getDayOfWeekInMonth()) : null);

        List<Integer> daysOfMonth =
                req.daysOfMonth() != null ? req.daysOfMonth()
                        : (frequency == RecurrenceFrequency.MONTHLY
                        && monthlyType == MonthlyType.DAY_OF_MONTH
                        ? baseRg.getDaysOfMonthAsList() : null);

        Integer monthOfYear =
                req.monthOfYear() != null ? req.monthOfYear()
                        : (frequency == RecurrenceFrequency.YEARLY ? baseRg.getMonthOfYear() : null);

        switch (frequency) {
            case NONE, DAILY -> {
                if (daysOfWeek != null || daysOfMonth != null
                        || weekOfMonth != null || dayOfWeekInMonth != null || monthlyType != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                if (req.frequency() == RecurrenceFrequency.DAILY) {
                    // 값이 범위를 벗어난 경우 (0 이하 또는 365 이상)
                    if (req.intervalValue() != null && (req.intervalValue() <= 0 || req.intervalValue() >= 365)) {
                        throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAILY_INTERVAL_VALUE);
                    }
                }
            }
            case WEEKLY -> {
                if (daysOfMonth != null || weekOfMonth != null
                        || dayOfWeekInMonth != null || monthlyType != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                if (req.intervalValue() != null && req.intervalValue() != 1) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_WEEKLY_INTERVAL_VALUE);
                }
            }
            case MONTHLY -> {
                if (daysOfWeek != null || monthOfYear != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                if (req.intervalValue() !=null && (req.intervalValue() <= 0 || req.intervalValue() >= 12)) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_MONTHLY_INTERVAL_VALUE);
                }
                if (monthlyType == MonthlyType.DAY_OF_MONTH
                        && (weekOfMonth != null || dayOfWeekInMonth != null || req.weekdayRule() != null)) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                if (req.monthlyType() == MonthlyType.DAY_OF_WEEK) {
                    if (req.daysOfMonth() != null) {
                        throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                    }

                    if (req.weekdayRule() == MonthlyWeekdayRule.SINGLE) {
                        if (req.dayOfWeekInMonth() != null && req.dayOfWeekInMonth().size() != 1) {
                            throw new RecurrenceGroupException(
                                    RecurrenceGroupErrorCode.INVALID_SIZE_OF_DAY_OF_WEEK_IN_MONTH
                            );
                        }
                    } else {
                        if (req.weekdayRule() != null && req.dayOfWeekInMonth() != null) {
                            throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAY_OF_WEEK_IN_MONTH);
                        }
                    }
                }
            }
            case YEARLY -> {
                if (daysOfWeek != null || weekOfMonth != null || daysOfMonth != null
                        || dayOfWeekInMonth != null || monthlyType != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_FREQUENCY_CONDITION);
                }
                if (req.intervalValue() != null && (req.intervalValue() <= 0 || req.intervalValue() >= 100)) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_YEARLY_INTERVAL_VALUE);
                }
            }
        }
    }

    private static final Set<DayOfWeek> VALID_DAYS =
            Set.of(DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY);

    private void validateWeeklyDays(RecurrenceGroupReqDTO.CreateReq req) {
        if (req.frequency() != RecurrenceFrequency.WEEKLY) return;

        if (req.daysOfWeek() == null || req.daysOfWeek().isEmpty()) return; // default 허용

        for (DayOfWeek day : req.daysOfWeek()) {
            if (!VALID_DAYS.contains(day)) {
                throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAY_OF_WEEK);
            }
        }
    }

    private void validateWeeklyDaysForUpdate(RecurrenceGroupReqDTO.UpdateReq req, RecurrenceGroup baseRg) {
        RecurrenceFrequency frequency =
                req.frequency() != null ? req.frequency() : baseRg.getFrequency();

        if (frequency != RecurrenceFrequency.WEEKLY) return;

        List<DayOfWeek> days =
                req.daysOfWeek() != null ? req.daysOfWeek() : toDayOfWeekList(baseRg.getDaysOfWeek());

        if (days.isEmpty()) return;

        for (DayOfWeek day : days) {
            if (!VALID_DAYS.contains(day)) {
                throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAY_OF_WEEK);
            }
        }
    }

    private void validateMonthlyDayOfWeek(RecurrenceGroupReqDTO.CreateReq req) {
        if (req.frequency() != RecurrenceFrequency.MONTHLY
                || req.monthlyType() != MonthlyType.DAY_OF_WEEK) return;

        List<DayOfWeek> days = req.dayOfWeekInMonth();

        if (days == null || days.isEmpty()) return;

        for (DayOfWeek day : req.dayOfWeekInMonth()) {
            if (!VALID_DAYS.contains(day)) {
                throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAY_OF_WEEK);
            }
        }
    }

    private void validateMonthlyDayOfWeekForUpdate(
            RecurrenceGroupReqDTO.UpdateReq req,
            RecurrenceGroup baseRg
    ) {
        RecurrenceFrequency frequency =
                req.frequency() != null ? req.frequency() : baseRg.getFrequency();

        MonthlyType monthlyType =
                req.monthlyType() != null ? req.monthlyType() : baseRg.getMonthlyType();

        if (frequency != RecurrenceFrequency.MONTHLY ||
                monthlyType != MonthlyType.DAY_OF_WEEK) return;

        List<DayOfWeek> days =
                req.dayOfWeekInMonth() != null
                        ? req.dayOfWeekInMonth()
                        : toDayOfWeekList(baseRg.getDayOfWeekInMonth());

        if (days.isEmpty()) return;

        for (DayOfWeek day : days) {
            if (!VALID_DAYS.contains(day)) {
                throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAY_OF_WEEK);
            }
        }
    }

    private static List<DayOfWeek> toDayOfWeekList(String daysOfWeek) {
        return Arrays.stream(daysOfWeek.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .toList();
    }
}
