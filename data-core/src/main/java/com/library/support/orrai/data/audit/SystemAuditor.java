package com.library.support.orrai.data.audit;

import java.util.Optional;

/**
 * Default {@link CurrentAuditor} that attributes all changes to a fixed {@code "system"} principal.
 * Registered by the data adapters only when the consumer has not supplied its own
 * {@link CurrentAuditor} bean.
 */
public class SystemAuditor implements CurrentAuditor {

    /** Auditor identifier used when no real user context is available. */
    public static final String SYSTEM = "system";

    @Override
    public Optional<String> currentAuditor() {
        return Optional.of(SYSTEM);
    }
}
