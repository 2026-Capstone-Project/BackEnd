package com.project.backend.domain.suggestion.invalidation.snapshot;

import com.project.backend.domain.suggestion.invalidation.fingerprint.GroupFingerprint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.PlanFingerprint;

/**
 * suggestion 무효화 비교용 snapshot 인터페이스
 */
public interface SuggestionInvalidationSnapshot<
        P extends PlanFingerprint,
        G extends GroupFingerprint
        > {

    byte[] planKeyHash();

    P planFingerprint();

    byte[] groupKeyHash();

    G groupFingerprint();
}