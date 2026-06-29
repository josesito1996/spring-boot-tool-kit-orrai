package com.library.support.orrai.data.repository;

import java.io.Serializable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Common base repository for the Spring Boot 2 adapter. Extend it from your domain repositories to
 * inherit the standard {@link JpaRepository} operations under a single project-wide abstraction
 * (a natural seam for adding shared query methods later).
 *
 * <p>{@code @NoRepositoryBean} so Spring Data does not try to instantiate this interface directly.
 *
 * @param <T>  entity type, typically a subclass of {@code BaseEntity}
 * @param <ID> identifier type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {
}
