package com.project.backend.domain.suggestion.invalidation.publisher;


import com.project.backend.domain.suggestion.enums.SuggestionInvalidateReason;
import com.project.backend.domain.suggestion.vo.SuggestionInvalidateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuggestionInvalidatePublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(Long memberId, SuggestionInvalidateReason reason, byte[] hash) {
        if (hash == null || hash.length == 0) return;
        publisher.publishEvent(new SuggestionInvalidateEvent(memberId, hash, reason));
    }
}