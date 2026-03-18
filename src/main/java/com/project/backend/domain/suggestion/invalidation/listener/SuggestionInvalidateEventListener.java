package com.project.backend.domain.suggestion.invalidation.listener;

import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import com.project.backend.domain.suggestion.vo.SuggestionInvalidateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Suggestion 무효화 이벤트를 받아 실제 무효화 처리
 */
@Component
@RequiredArgsConstructor
public class SuggestionInvalidateEventListener {

    private final SuggestionRepository suggestionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(SuggestionInvalidateEvent event) {
        suggestionRepository.bulkInvalidateOne(
                event.memberId(),
                event.targetKeyHash(),
                event.reason()
        );
    }
}