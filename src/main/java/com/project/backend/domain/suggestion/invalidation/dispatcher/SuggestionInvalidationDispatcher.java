package com.project.backend.domain.suggestion.invalidation.dispatcher;

import com.project.backend.domain.suggestion.invalidation.planner.InvalidationCommand;
import com.project.backend.domain.suggestion.invalidation.planner.InvalidationPlan;
import com.project.backend.domain.suggestion.invalidation.publisher.SuggestionInvalidatePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Planner가 만든 무효화 계획을 실제 발행하는 dispatcher
 */
@Component
@RequiredArgsConstructor
public class SuggestionInvalidationDispatcher {

    private final SuggestionInvalidatePublisher suggestionInvalidatePublisher;

    public void dispatch(Long memberId, InvalidationPlan plan) {
        // 무효화할 대상이 없으면 종료
        if (plan == null || plan.isEmpty()) {
            return;
        }

        // 각 command를 publisher에 전달
        for (InvalidationCommand command : plan.commands()) {
            suggestionInvalidatePublisher.publish(
                    memberId,
                    command.reason(),
                    command.targetHash()
            );
        }
    }
}