package com.library.support.orrai.data.sample;

import org.springframework.data.jpa.repository.JpaRepository;

/** Test fixture repository for {@link CustomIdAuditableEntity}. */
public interface CustomIdAuditableRepository extends JpaRepository<CustomIdAuditableEntity, Long> {
}