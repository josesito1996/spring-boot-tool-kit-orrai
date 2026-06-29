package com.library.support.orrai.data.page;

/**
 * Shared pagination defaults and bounds, applied by {@link PageQuery} when building a query.
 */
public final class PageDefaults {

    /** Zero-based index of the first page. */
    public static final int DEFAULT_PAGE = 0;

    /** Page size used when the caller does not specify one. */
    public static final int DEFAULT_SIZE = 20;

    /** Upper bound on page size, to protect the data store from unbounded reads. */
    public static final int MAX_PAGE_SIZE = 200;

    private PageDefaults() {
    }
}
