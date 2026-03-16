package com.project.backend.domain.suggestion.invalidation.planner;

import java.util.List;

public record InvalidationPlan(
        List<InvalidationCommand> commands
) {

    public static InvalidationPlan empty() {
        return new InvalidationPlan(List.of());
    }

    public boolean isEmpty() {
        return commands == null || commands.isEmpty();
    }
}
