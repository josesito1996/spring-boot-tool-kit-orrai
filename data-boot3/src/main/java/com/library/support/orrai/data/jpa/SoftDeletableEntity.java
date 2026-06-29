package com.library.support.orrai.data.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Adds logical (soft) deletion to {@link AuditableEntity} for the Spring Boot 3 (jakarta) adapter.
 * Rows are flagged via {@link #markDeleted()} rather than physically removed.
 *
 * <p>Filtering deleted rows is done explicitly through the {@code *ByDeletedFalse} query methods on
 * {@code SoftDeleteRepository}. We deliberately avoid Hibernate's {@code @Where} here because it is
 * not honored on a {@code @MappedSuperclass} and behaves inconsistently across Hibernate 5 and 6.
 */
@MappedSuperclass
public abstract class SoftDeletableEntity extends AuditableEntity {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    /** Flags this entity as deleted and stamps the deletion time (idempotent). */
    public void markDeleted() {
        if (!deleted) {
            this.deleted = true;
            this.deletedAt = Instant.now();
        }
    }

    /** Reverts a soft deletion, clearing the flag and timestamp. */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
    }
}
