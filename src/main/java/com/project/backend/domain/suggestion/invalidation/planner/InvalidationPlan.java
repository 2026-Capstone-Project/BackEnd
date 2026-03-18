package com.project.backend.domain.suggestion.invalidation.planner;

import java.util.List;

/**
 * suggestion 무효화 명령 목록
 */
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