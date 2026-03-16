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

@Slf4j
@Component
public class SuggestionInvalidationPlanner {

    public <P extends PlanFingerprint, G extends GroupFingerprint>
    InvalidationPlan planForUpdate(
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
        log.info("SuggestionInvalidatePlanner : before: {}, after: {}", before, after);

        boolean planKeyChanged = !Arrays.equals(before.planKeyHash(), after.planKeyHash());
        boolean groupKeyChanged = !Arrays.equals(before.groupKeyHash(), after.groupKeyHash());
        log.info("SuggestionInvalidatePlanner : planKeyChanged: {}, groupKeyChanged: {}", planKeyChanged, groupKeyChanged);

        boolean planFingerprintChanged = !Objects.equals(
                before.planFingerprint(),
                after.planFingerprint()
        );
        boolean groupFingerprintChanged = !Objects.equals(
                before.groupFingerprint(),
                after.groupFingerprint()
        );
        log.info("SuggestionInvalidatePlanner : planFingerprintChanged: {}, groupFingerprintChanged: {}", planFingerprintChanged, groupFingerprintChanged);

        boolean invalidatePlanAxis = planKeyChanged || planFingerprintChanged;
        boolean invalidateGroupAxis = groupKeyChanged || groupFingerprintChanged;

        if (invalidatePlanAxis && after.planKeyHash() != null) {
            commands.add(new InvalidationCommand(planUpdatedReason, after.planKeyHash()));

            if (planKeyChanged && before.planKeyHash() != null) {
                commands.add(new InvalidationCommand(planUpdatedReason, before.planKeyHash()));
            }
        }

        if (invalidateGroupAxis) {
            if (after.groupKeyHash() != null) {
                commands.add(new InvalidationCommand(afterGroupReason, after.groupKeyHash()));
            }

            if (groupKeyChanged && before.groupKeyHash() != null) {
                commands.add(new InvalidationCommand(beforeGroupReason, before.groupKeyHash()));
            }
        }
        log.info("SuggestionInvalidatePlanner : commands: {}", commands);
        return commands.isEmpty()
                ? InvalidationPlan.empty()
                : new InvalidationPlan(commands);
    }
}
