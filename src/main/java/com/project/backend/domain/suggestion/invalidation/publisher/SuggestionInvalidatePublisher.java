package com.project.backend.domain.suggestion.invalidation.publisher;

import com.project.backend.domain.suggestion.enums.SuggestionInvalidateReason;
import com.project.backend.domain.suggestion.vo.SuggestionInvalidateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * suggestion 무효화 이벤트 발행
 */
@Component
@RequiredArgsConstructor
public class SuggestionInvalidatePublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(Long memberId, SuggestionInvalidateReason reason, byte[] hash) {
        // 유효한 hash가 있을 때만 이벤트 발행
        if (hash == null || hash.length == 0) return;

        publisher.publishEvent(new SuggestionInvalidateEvent(memberId, hash, reason));
    }
}