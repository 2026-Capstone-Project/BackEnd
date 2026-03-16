package com.project.backend.domain.suggestion.invalidation.snapshot;

import com.project.backend.domain.suggestion.invalidation.fingerprint.GroupFingerprint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.PlanFingerprint;

public interface SuggestionInvalidationSnapshot<
        P extends PlanFingerprint,
        G extends GroupFingerprint
> {

    byte[] planKeyHash();

    P planFingerprint();

    byte[] groupKeyHash();

    G groupFingerprint();
}