package com.library.support.orrai.data.jpa;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;

/**
 * Base JPA entity for the Spring Boot 3 (jakarta) adapter: a surrogate {@code Long} identity with
 * {@code IDENTITY} generation, plus identity-based {@code equals}/{@code hashCode} safe for use in
 * collections before and after persistence.
 *
 * <p>Two entities are equal only when both have the same non-null id; transient entities (null id)
 * are equal only by reference, which avoids the classic "all new entities collide in a HashSet"
 * pitfall.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** @return {@code true} until the entity has been assigned a persistent identity. */
    public boolean isNew() {
        return id == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity)) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        // Constant for transient entities; stable for managed ones. Keeps HashSet semantics correct
        // across the transient -> managed transition.
        return Objects.hashCode(getClass());
    }
}
