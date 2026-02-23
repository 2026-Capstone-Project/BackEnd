package com.project.backend.domain.suggestion.publisher;


import com.project.backend.domain.suggestion.enums.SuggestionInvalidateReason;
import com.project.backend.domain.suggestion.util.SuggestionTargetKeyUtil;
import com.project.backend.domain.suggestion.util.TargetKeyHashUtil;
import com.project.backend.domain.suggestion.vo.SuggestionInvalidateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class SuggestionInvalidatePublisher {

    private final ApplicationEventPublisher publisher;

    // ===== hash builders =====
    public byte[] eventHash(String title, String location) {
        return TargetKeyHashUtil.sha256(SuggestionTargetKeyUtil.eventKey(title, location));
    }

    public byte[] todoHash(String title, String identifier) {
        return TargetKeyHashUtil.sha256(SuggestionTargetKeyUtil.todoKey(title, identifier));
    }

    public byte[] rgHash(Long rgId) {
        return TargetKeyHashUtil.sha256(SuggestionTargetKeyUtil.rgKey(rgId));
    }

    public byte[] trgHash(Long trgId) {
        return TargetKeyHashUtil.sha256(SuggestionTargetKeyUtil.trgKey(trgId));
    }

    // ===== publish one hash =====
    public void publish(Long memberId, SuggestionInvalidateReason reason, byte[] hash) {
        if (hash == null || hash.length == 0) return;
        publisher.publishEvent(new SuggestionInvalidateEvent(memberId, hash, reason));
    }
}