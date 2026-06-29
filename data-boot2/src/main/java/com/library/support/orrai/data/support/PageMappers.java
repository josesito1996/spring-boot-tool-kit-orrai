package com.library.support.orrai.data.support;

import com.library.support.orrai.data.page.PageQuery;
import com.library.support.orrai.data.page.PageResponse;
import com.library.support.orrai.data.page.SortOrder;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Bridges the framework-agnostic {@link PageQuery}/{@link PageResponse} DTOs and Spring Data's
 * {@link Pageable}/{@link Page}, so application code can depend only on the {@code data-core}
 * abstractions while repositories still receive a {@link Pageable}.
 */
public final class PageMappers {

    private PageMappers() {
    }

    /** Converts an agnostic {@link PageQuery} into a Spring Data {@link Pageable}. */
    public static Pageable toPageable(PageQuery query) {
        return PageRequest.of(query.getPage(), query.getSize(), toSort(query.getSort()));
    }

    /** Converts agnostic {@link SortOrder}s into a Spring Data {@link Sort}. */
    public static Sort toSort(List<SortOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> springOrders = orders.stream()
                .map(order -> new Sort.Order(toDirection(order), order.getProperty()))
                .collect(Collectors.toList());
        return Sort.by(springOrders);
    }

    /** Converts a Spring Data {@link Page} into an agnostic {@link PageResponse}. */
    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        PageQuery query = PageQuery.of(page.getNumber(), page.getSize());
        return PageResponse.of(page.getContent(), query, page.getTotalElements());
    }

    private static Sort.Direction toDirection(SortOrder order) {
        return order.getDirection() == com.library.support.orrai.data.page.Direction.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }
}
