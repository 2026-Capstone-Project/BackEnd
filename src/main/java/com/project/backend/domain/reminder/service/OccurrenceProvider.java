package com.project.backend.domain.reminder.service;

import com.project.backend.domain.event.service.query.EventQueryService;
import com.project.backend.domain.reminder.dto.NextOccurrenceResult;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.todo.service.query.TodoQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OccurrenceProvider {

    private final EventQueryService eventQueryService;
    private final TodoQueryService todoService;

    public NextOccurrenceResult getNextOccurrence(Reminder reminder) {

        if (reminder.getTargetType() == TargetType.EVENT) {
            return eventQueryService.calculateNextOccurrence(reminder);
        }

        return todoService.calculateNextOccurrence(reminder);
    }
}