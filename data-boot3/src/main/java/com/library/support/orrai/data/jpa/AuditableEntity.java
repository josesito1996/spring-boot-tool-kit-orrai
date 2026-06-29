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
 * Adds automatic creation/modification auditing to {@link BaseEntity} for the Spring Boot 3
 * (jakarta) adapter. The {@code createdAt}/{@code createdBy} columns are populated on insert and the
 * {@code updatedAt}/{@code updatedBy} columns on every update, driven by Spring Data JPA auditing.
 *
 * <p>Requires JPA auditing to be active: it is auto-configured by
 * {@code OrraiJpaAuditingAutoConfiguration} (enabled by default; disable with
 * {@code orrai.jpa.auditing.enabled=false}). The {@code *By} columns are filled from the
 * registered {@code CurrentAuditor}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity extends BaseEntity {

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
