package com.project.backend.domain.suggestion.invalidation.dispatcher;

import com.project.backend.domain.suggestion.invalidation.planner.InvalidationCommand;
import com.project.backend.domain.suggestion.invalidation.planner.InvalidationPlan;
import com.project.backend.domain.suggestion.invalidation.publisher.SuggestionInvalidatePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuggestionInvalidationDispatcher {

    private final SuggestionInvalidatePublisher suggestionInvalidatePublisher;

    public void dispatch(Long memberId, InvalidationPlan plan) {
        if (plan == null || plan.isEmpty()) {
            return;
        }

        for (InvalidationCommand command : plan.commands()) {
            suggestionInvalidatePublisher.publish(
                    memberId,
                    command.reason(),
                    command.targetHash()
            );
        }
    }
}
