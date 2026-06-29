package com.library.support.orrai.data.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.library.support.orrai.data.autoconfigure.OrraiJpaAuditingAutoConfiguration;
import com.library.support.orrai.data.sample.CustomIdAuditableEntity;
import com.library.support.orrai.data.sample.CustomIdAuditableRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Verifies, against H2, that {@code AuditableFields} adds auditing to an entity that owns its own
 * {@code @Id} (no {@code BaseEntity} inheritance, hence no duplicate-identifier conflict) and that
 * the audit columns are populated on insert and refreshed on update.
 */
@DataJpaTest
@Import(OrraiJpaAuditingAutoConfiguration.class)
class AuditableFieldsTest {

    @Autowired
    private CustomIdAuditableRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("entity with its own id persists and inherits the audit columns without conflict")
    void persist_withOwnId_populatesAuditColumns() {
        // Act
        CustomIdAuditableEntity saved = repository.saveAndFlush(new CustomIdAuditableEntity("alpha"));

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("system");
        assertThat(saved.getUpdatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("updating a managed entity advances updatedAt while preserving createdAt")
    void update_refreshesUpdatedAtAndKeepsCreatedAt() {
        // Arrange
        Long id = repository.saveAndFlush(new CustomIdAuditableEntity("beta")).getId();
        entityManager.clear();
        // Read createdAt back from the database so we compare values at the same (DB) precision.
        CustomIdAuditableEntity reloaded = repository.findById(id).orElseThrow();
        java.time.Instant createdAt = reloaded.getCreatedAt();

        // Act
        reloaded.setName("beta-renamed");
        CustomIdAuditableEntity updated = repository.saveAndFlush(reloaded);

        // Assert
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }
}