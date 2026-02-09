package com.project.backend.domain.reminder.provider;

import com.project.backend.domain.briefing.dto.TodayOccurrenceResult;
import com.project.backend.domain.event.service.query.EventQueryService;
import com.project.backend.domain.reminder.dto.NextOccurrenceResult;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.todo.service.query.TodoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OccurrenceProvider {

    private final EventQueryService eventQueryService;
    private final TodoQueryService todoService;

    public NextOccurrenceResult getNextOccurrence(Reminder reminder) {
        if (reminder.getTargetType() == TargetType.EVENT) {
            return eventQueryService.calculateNextOccurrence(reminder.getTargetId(), reminder.getOccurrenceTime());
        }

        return todoService.calculateNextOccurrence(reminder);
    }

    public List<TodayOccurrenceResult> getTodayOccurrence(TargetType type, List<Long> targetId, LocalDate date) {
        if (type == TargetType.EVENT) {
            return eventQueryService.calculateTodayOccurrence(targetId, date);
        }
        return todoService.calculateTodayOccurrence(targetId, date);
    }
}