package com.project.backend.domain.suggestion.invalidation.planner;

import com.project.backend.domain.suggestion.enums.SuggestionInvalidateReason;
import com.project.backend.domain.suggestion.invalidation.fingerprint.GroupFingerprint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.PlanFingerprint;
import com.project.backend.domain.suggestion.invalidation.snapshot.SuggestionInvalidationSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * before/after snapshot 비교로 suggestion 무효화 계획 생성
 */
@Slf4j
@Component
public class SuggestionInvalidationPlanner {

    public <P extends PlanFingerprint, G extends GroupFingerprint> InvalidationPlan planForCreate(
            SuggestionInvalidationSnapshot<P, G> after,
            SuggestionInvalidateReason planCreatedReason
    ) {
        if (after == null || after.planKeyHash() == null) {
            return InvalidationPlan.empty();
        }

        return new InvalidationPlan(List.of(
                new InvalidationCommand(planCreatedReason, after.planKeyHash())
        ));
    }

    public <P extends PlanFingerprint, G extends GroupFingerprint> InvalidationPlan planForUpdate(
            SuggestionInvalidationSnapshot<P, G> before,
            SuggestionInvalidationSnapshot<P, G> after,
            SuggestionInvalidateReason planUpdatedReason,
            SuggestionInvalidateReason beforeGroupReason,
            SuggestionInvalidateReason afterGroupReason
    ) {
        if (before == null || after == null) {
            return InvalidationPlan.empty();
        }

        List<InvalidationCommand> commands = new ArrayList<>();
        log.info("SuggestionInvalidationPlanner - before: {}, after: {}", before, after);

        // 개별 대상 / 그룹 대상 key 변경 여부
        boolean planKeyChanged = !Arrays.equals(before.planKeyHash(), after.planKeyHash());
        boolean groupKeyChanged = !Arrays.equals(before.groupKeyHash(), after.groupKeyHash());
        log.info("SuggestionInvalidationPlanner - planKeyChanged: {}, groupKeyChanged: {}", planKeyChanged, groupKeyChanged);

        // 개별 대상 / 그룹 대상 fingerprint 변경 여부
        boolean planFingerprintChanged = !Objects.equals(before.planFingerprint(), after.planFingerprint());
        boolean groupFingerprintChanged = !Objects.equals(before.groupFingerprint(), after.groupFingerprint());
        log.info(
                "SuggestionInvalidationPlanner - planFingerprintChanged: {}, groupFingerprintChanged: {}",
                planFingerprintChanged,
                groupFingerprintChanged
        );

        boolean invalidatePlanAxis = planKeyChanged || planFingerprintChanged;
        boolean invalidateGroupAxis = groupKeyChanged || groupFingerprintChanged;

        if (invalidatePlanAxis && after.planKeyHash() != null) {
            commands.add(new InvalidationCommand(planUpdatedReason, after.planKeyHash()));

            // key가 바뀌었으면 기존 key 기준 suggestion도 함께 무효화
            if (planKeyChanged && before.planKeyHash() != null) {
                commands.add(new InvalidationCommand(planUpdatedReason, before.planKeyHash()));
            }
        }

        if (invalidateGroupAxis) {
            if (after.groupKeyHash() != null) {
                commands.add(new InvalidationCommand(afterGroupReason, after.groupKeyHash()));
            }

            // 그룹 key가 바뀌었으면 기존 그룹 key 기준 suggestion도 함께 무효화
            if (groupKeyChanged && before.groupKeyHash() != null) {
                commands.add(new InvalidationCommand(beforeGroupReason, before.groupKeyHash()));
            }
        }

        log.info("SuggestionInvalidationPlanner - commands: {}", commands);

        return commands.isEmpty()
                ? InvalidationPlan.empty()
                : new InvalidationPlan(commands);
    }

    public <P extends PlanFingerprint, G extends GroupFingerprint> InvalidationPlan planForDelete(
            SuggestionInvalidationSnapshot<P, G> before,
            SuggestionInvalidateReason planDeletedReason,
            SuggestionInvalidateReason groupReason,
            boolean invalidatePlanAxis,
            boolean invalidateGroupAxis
    ) {
        if (before == null) {
            return InvalidationPlan.empty();
        }

        List<InvalidationCommand> commands = new ArrayList<>();

        if (invalidatePlanAxis && before.planKeyHash() != null) {
            commands.add(new InvalidationCommand(planDeletedReason, before.planKeyHash()));
        }

        if (invalidateGroupAxis && before.groupKeyHash() != null) {
            commands.add(new InvalidationCommand(groupReason, before.groupKeyHash()));
        }

        return commands.isEmpty()
                ? InvalidationPlan.empty()
                : new InvalidationPlan(commands);
    }

    public InvalidationPlan planForSingleTarget(
            SuggestionInvalidateReason reason,
            byte[] targetHash
    ) {
        if (targetHash == null) {
            return InvalidationPlan.empty();
        }

        return new InvalidationPlan(List.of(
                new InvalidationCommand(reason, targetHash)
        ));
    }
}