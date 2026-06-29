package com.library.support.orrai.data.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Framework-agnostic, immutable page of results: the content plus pagination metadata. Use it as
 * the response envelope for paginated REST endpoints so the shape is consistent across services
 * and independent of Spring Data's {@code Page}.
 *
 * @param <T> element type of the page content
 */
public final class PageResponse<T> {

    private final List<T> content;

    private final int page;

    private final int size;

    private final long totalElements;

    private final int totalPages;

    private final boolean first;

    private final boolean last;

    private final boolean empty;

    private PageResponse(List<T> content, int page, int size, long totalElements, int totalPages,
                         boolean first, boolean last, boolean empty) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
        this.empty = empty;
    }

    /**
     * Builds a response from the page content, the originating {@link PageQuery}, and the total
     * number of matching elements. Derives {@code totalPages}, {@code first}, {@code last} and
     * {@code empty}.
     */
    public static <T> PageResponse<T> of(List<T> content, PageQuery query, long totalElements) {
        List<T> safeContent = content == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(content));
        int page = query.getPage();
        int size = query.getSize();
        long safeTotal = Math.max(totalElements, 0);
        int totalPages = size == 0 ? 0 : (int) ((safeTotal + size - 1) / size);
        boolean first = page == 0;
        boolean last = page >= totalPages - 1;
        return new PageResponse<>(safeContent, page, size, safeTotal, totalPages, first, last,
                safeContent.isEmpty());
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }

    public boolean isEmpty() {
        return empty;
    }
}
