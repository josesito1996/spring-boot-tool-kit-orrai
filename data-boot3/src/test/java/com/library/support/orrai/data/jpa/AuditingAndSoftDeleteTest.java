package com.library.support.orrai.data.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.library.support.orrai.data.autoconfigure.OrraiJpaAuditingAutoConfiguration;
import com.library.support.orrai.data.sample.SampleEntity;
import com.library.support.orrai.data.sample.SampleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Verifies, against H2, that JPA auditing populates the audit columns on insert and that the
 * {@code @Where} soft-delete filter hides logically deleted rows from reads.
 */
@DataJpaTest
@Import(OrraiJpaAuditingAutoConfiguration.class)
class AuditingAndSoftDeleteTest {

    @Autowired
    private SampleRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("auditing fills createdAt and createdBy with the default system auditor on insert")
    void persist_populatesAuditColumns() {
        // Act
        SampleEntity saved = repository.saveAndFlush(new SampleEntity("alpha"));

        // Assert
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("soft-deleted rows are excluded from the active finders but the row is retained")
    void softDelete_hidesRowFromActiveFinders() {
        // Arrange
        SampleEntity persisted = repository.saveAndFlush(new SampleEntity("beta"));
        Long id = persisted.getId();

        // Act
        repository.softDelete(persisted);
        entityManager.flush();
        entityManager.clear();

        // Assert: hidden from soft-delete-aware reads...
        assertThat(repository.findByIdAndDeletedFalse(id)).isEmpty();
        assertThat(repository.findAllByDeletedFalse()).isEmpty();
        assertThat(repository.countByDeletedFalse()).isZero();
        // ...but physically retained and flagged.
        assertThat(repository.findById(id)).isPresent();
        assertThat(repository.findById(id).get().isDeleted()).isTrue();
        assertThat(repository.findById(id).get().getDeletedAt()).isNotNull();
    }
}
