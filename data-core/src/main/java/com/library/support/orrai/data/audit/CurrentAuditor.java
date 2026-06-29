package com.library.support.orrai.data.audit;

import java.util.Optional;

/**
 * Extension point that supplies the identity of the current user for JPA auditing
 * ({@code @CreatedBy} / {@code @LastModifiedBy}). Framework-agnostic so it can be backed by Spring
 * Security, an MDC value, a request header, etc.
 *
 * <p>The data adapters wrap this in an {@code AuditorAware<String>} bean. Provide your own
 * {@code CurrentAuditor} bean to override the default {@link SystemAuditor}.
 */
@FunctionalInterface
public interface CurrentAuditor {

    /**
     * @return the current auditor identifier, or {@link Optional#empty()} when none is available
     *         (e.g. background jobs); auditing columns are then left null.
     */
    Optional<String> currentAuditor();
}
