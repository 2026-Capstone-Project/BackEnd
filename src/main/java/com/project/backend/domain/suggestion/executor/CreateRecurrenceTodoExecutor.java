package com.project.backend.domain.suggestion.executor;

import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * TodoRecurrenceGroup Suggestion인 경우
 */
@Component
public class CreateRecurrenceTodoExecutor implements SuggestionExecutor {
    @Override
    public boolean supports(Category category) {
        return false;
    }

    @Override
    public void execute(Suggestion suggestion, Status currentStatus, Long memberId) {
        TodoRecurrenceGroup trg = suggestion.getTodoRecurrenceGroup();
        switch (trg.getEndType()) {
            case END_BY_DATE -> extendEndDate(trg);
            case END_BY_COUNT -> extendOccurrenceCount(trg);
            default -> throw new IllegalStateException("지원하지 않는 종료 타입");
        }
    }

    private void extendEndDate(TodoRecurrenceGroup trg) {
        LocalDate startDate = trg.getTodo().getStartDate();
        LocalDate endDate = trg.getEndDate();

        long dayDiff = ChronoUnit.DAYS.between(startDate, endDate);
        trg.extendEndDate(dayDiff);
    }

    private void extendOccurrenceCount(TodoRecurrenceGroup trg) {
        int occurrenceCount = trg.getOccurrenceCount();
        trg.updateOccurrenceCount(occurrenceCount * 2);
    }
}
