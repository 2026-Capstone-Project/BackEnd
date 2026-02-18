package com.project.backend.domain.suggestion.listener;

import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import com.project.backend.domain.suggestion.vo.SuggestionInvalidateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SuggestionInvalidateEventListener {

    private final SuggestionRepository suggestionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(SuggestionInvalidateEvent event) {
        suggestionRepository.bulkInvalidateOne(event.memberId(), event.targetKeyHash());
    }
}
