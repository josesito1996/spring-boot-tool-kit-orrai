package com.library.support.orrai.data.page;

/**
 * Sort direction for a {@link SortOrder}. Framework-agnostic: the Spring Boot adapters map this
 * onto {@code org.springframework.data.domain.Sort.Direction}.
 */
public enum Direction {
    ASC,
    DESC
}
