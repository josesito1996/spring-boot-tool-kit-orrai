package com.library.support.orrai.data.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Framework-agnostic, validated pagination request: a zero-based page index, a bounded page size,
 * and an ordered list of {@link SortOrder}s. Immutable; the sort list is defensively copied.
 *
 * <p>The Spring Boot data adapters convert this into {@code org.springframework.data.domain.Pageable}
 * via {@code PageMappers}, so consuming code never has to depend on Spring Data directly.
 */
public final class PageQuery {

    private final int page;

    private final int size;

    private final List<SortOrder> sort;

    private PageQuery(int page, int size, List<SortOrder> sort) {
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    /** Default first page with default size and no sorting. */
    public static PageQuery firstPage() {
        return of(PageDefaults.DEFAULT_PAGE, PageDefaults.DEFAULT_SIZE);
    }

    public static PageQuery of(int page, int size) {
        return of(page, size, Collections.emptyList());
    }

    /**
     * Builds a validated query. {@code page} is clamped to be non-negative, {@code size} is clamped
     * into {@code [1, MAX_PAGE_SIZE]}, and {@code sort} is copied (null entries rejected).
     */
    public static PageQuery of(int page, int size, List<SortOrder> sort) {
        int safePage = Math.max(page, PageDefaults.DEFAULT_PAGE);
        int safeSize = clampSize(size);
        List<SortOrder> safeSort = copySort(sort);
        return new PageQuery(safePage, safeSize, safeSort);
    }

    private static int clampSize(int size) {
        if (size < 1) {
            return PageDefaults.DEFAULT_SIZE;
        }
        return Math.min(size, PageDefaults.MAX_PAGE_SIZE);
    }

    private static List<SortOrder> copySort(List<SortOrder> sort) {
        if (sort == null || sort.isEmpty()) {
            return Collections.emptyList();
        }
        List<SortOrder> copy = new ArrayList<>(sort.size());
        for (SortOrder order : sort) {
            copy.add(Objects.requireNonNull(order, "Sort order must not be null"));
        }
        return Collections.unmodifiableList(copy);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    /** Zero-based offset of the first row of this page, useful for JDBC-style paging. */
    public long getOffset() {
        return (long) page * size;
    }

    public List<SortOrder> getSort() {
        return sort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageQuery)) {
            return false;
        }
        PageQuery that = (PageQuery) o;
        return page == that.page && size == that.size && sort.equals(that.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size, sort);
    }

    @Override
    public String toString() {
        return "PageQuery{page=" + page + ", size=" + size + ", sort=" + sort + '}';
    }
}
