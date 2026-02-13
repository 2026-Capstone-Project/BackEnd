package com.project.backend.domain.suggestion.executor;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * RecurrenceGroup Suggestion인 경우
 */
@Component
public class CreateRecurrenceEventExecutor implements SuggestionExecutor {

    @Override
    public boolean supports(Category category) {
        return false;
    }

    @Override
    public void execute(Suggestion suggestion, Status currentStatus) {
        RecurrenceGroup rg = suggestion.getRecurrenceGroup();

        switch (rg.getEndType()) {
            case END_BY_DATE -> extendEndDate(rg);
            case END_BY_COUNT -> extendOccurrenceCount(rg);
            default -> throw new IllegalStateException("지원하지 않는 종료 타입");
        }
    }

    private void extendEndDate(RecurrenceGroup rg) {
        LocalDate startDate = rg.getEvent().getStartTime().toLocalDate();
        LocalDate endDate = rg.getEndDate();

        long dayDiff = ChronoUnit.DAYS.between(startDate, endDate);
        rg.extendEndDate(dayDiff);
    }

    private void extendOccurrenceCount(RecurrenceGroup rg) {
        int occurrenceCount = rg.getOccurrenceCount();
        rg.updateOccurrenceCount(occurrenceCount * 2);
    }
}
