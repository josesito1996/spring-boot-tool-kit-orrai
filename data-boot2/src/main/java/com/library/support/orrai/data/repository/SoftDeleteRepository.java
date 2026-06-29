package com.library.support.orrai.data.repository;

import com.library.support.orrai.data.jpa.SoftDeletableEntity;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Base repository for {@link SoftDeletableEntity} types in the Spring Boot 2 adapter. Deletions are
 * logical: {@link #softDelete(SoftDeletableEntity)} flags the row instead of removing it, and the
 * {@code *ByDeletedFalse} finders return only the still-active rows.
 *
 * <p>The inherited {@link org.springframework.data.jpa.repository.JpaRepository} methods
 * ({@code findAll}, {@code findById}, ...) still see <em>all</em> rows including deleted ones; use
 * the active finders below for soft-delete-aware reads.
 *
 * @param <T>  soft-deletable entity type
 * @param <ID> identifier type
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T extends SoftDeletableEntity, ID extends Serializable>
        extends BaseRepository<T, ID> {

    /** @return all rows that have not been soft-deleted. */
    List<T> findAllByDeletedFalse();

    /** @return a page of rows that have not been soft-deleted. */
    Page<T> findAllByDeletedFalse(Pageable pageable);

    /** @return the active (non-deleted) row with the given id, if any. */
    Optional<T> findByIdAndDeletedFalse(ID id);

    /** @return the number of rows that have not been soft-deleted. */
    long countByDeletedFalse();

    /** Marks the entity as deleted and persists the change. */
    default T softDelete(T entity) {
        entity.markDeleted();
        return save(entity);
    }
}
