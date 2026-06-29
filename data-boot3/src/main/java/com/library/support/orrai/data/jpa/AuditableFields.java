package com.library.support.orrai.data.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Standalone creation/modification auditing for the Spring Boot 3 (jakarta) adapter, decoupled from
 * any identity strategy. Unlike {@link AuditableEntity}, this superclass declares <strong>no</strong>
 * {@code @Id}, so it can be applied to entities that already own their primary key (any name, type,
 * or generation strategy) without the duplicate-identifier conflict that arises when extending
 * {@link BaseEntity}.
 *
 * <p>The {@code createdAt}/{@code createdBy} columns are populated on insert and the
 * {@code updatedAt}/{@code updatedBy} columns on every update, driven by Spring Data JPA auditing.
 *
 * <p>Requires JPA auditing to be active: it is auto-configured by
 * {@code OrraiJpaAuditingAutoConfiguration} (enabled by default; disable with
 * {@code orrai.jpa.auditing.enabled=false}). The {@code *By} columns are filled from the registered
 * {@code CurrentAuditor}.
 *
 * <p>Use {@link AuditableEntity} instead when you also want the toolkit-provided {@code Long}
 * identity and identity-based {@code equals}/{@code hashCode}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableFields {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}