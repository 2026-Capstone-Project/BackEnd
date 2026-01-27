package com.project.backend.domain.event.validator;

import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.event.exception.RecurrenceGroupErrorCode;
import com.project.backend.domain.event.exception.RecurrenceGroupException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class RecurrenceGroupValidator {

    public void validateCreate(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time) {
        validateEndType(req, time);
        validateFrequencyRule(req);
        validateWeeklyDays(req);
        validateMonthlyDayOfWeek(req);
    }

    public void validateEndType(RecurrenceGroupReqDTO.CreateReq req, LocalDateTime time) {
        switch (req.endType()) {
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

    public void validateFrequencyRule(RecurrenceGroupReqDTO.CreateReq req) {
        switch (req.frequency()) {
            case NONE, DAILY -> {
                if (req.daysOfWeek() != null || req.daysOfMonth() != null
                        || req.weekOfMonth() != null || req.dayOfWeekInMonth() != null || req.monthlyType() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
            }
            case WEEKLY -> {
                if (req.daysOfMonth() != null || req.weekOfMonth() != null
                        || req.dayOfWeekInMonth() != null || req.monthlyType() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
            }
            case MONTHLY -> {
                if ( req.daysOfWeek() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
            }
            case YEARLY -> {
                if (req.daysOfWeek() != null || req.weekOfMonth() != null
                        || req.dayOfWeekInMonth() != null || req.monthlyType() != null) {
                    throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_END_CONDITION);
                }
            }
            default -> throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_MONTHLY_TYPE);
        }
    }

    private static final Set<String> VALID_DAYS =
            Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    private void validateWeeklyDays(RecurrenceGroupReqDTO.CreateReq req) {
        if (req.frequency() != RecurrenceFrequency.WEEKLY) return;

        if (req.daysOfWeek() == null || req.daysOfWeek().isEmpty()) return; // default 허용

        for (String day : req.daysOfWeek()) {
            if (!VALID_DAYS.contains(day)) {
                throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAY_OF_WEEK);
            }
        }
    }
    private void validateMonthlyDayOfWeek(RecurrenceGroupReqDTO.CreateReq req) {
        if (req.frequency() != RecurrenceFrequency.MONTHLY || req.monthlyType() != MonthlyType.DAY_OF_WEEK) return;

        if (req.dayOfWeekInMonth() == null || req.dayOfWeekInMonth().isEmpty()) return;

        for (String day : req.dayOfWeekInMonth()) {
            if (!VALID_DAYS.contains(day)) {
                throw new RecurrenceGroupException(RecurrenceGroupErrorCode.INVALID_DAY_OF_WEEK);
            }
        }
    }


}
