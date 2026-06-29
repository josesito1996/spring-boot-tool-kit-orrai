package com.library.support.orrai.data.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    @DisplayName("clamps negative page to zero")
    void of_negativePage_clampedToZero() {
        // Act
        PageQuery query = PageQuery.of(-3, 10);

        // Assert
        assertEquals(0, query.getPage());
    }

    @Test
    @DisplayName("falls back to default size when size is not positive")
    void of_nonPositiveSize_usesDefault() {
        // Act
        PageQuery query = PageQuery.of(0, 0);

        // Assert
        assertEquals(PageDefaults.DEFAULT_SIZE, query.getSize());
    }

    @Test
    @DisplayName("caps size at the configured maximum")
    void of_oversizedSize_cappedAtMax() {
        // Act
        PageQuery query = PageQuery.of(0, PageDefaults.MAX_PAGE_SIZE + 50);

        // Assert
        assertEquals(PageDefaults.MAX_PAGE_SIZE, query.getSize());
    }

    @Test
    @DisplayName("computes a zero-based offset from page and size")
    void getOffset_computesPageTimesSize() {
        // Act
        PageQuery query = PageQuery.of(3, 25);

        // Assert
        assertEquals(75L, query.getOffset());
    }

    @Test
    @DisplayName("defensively copies the sort list so the query stays immutable")
    void of_sortList_isDefensivelyCopied() {
        // Arrange
        List<SortOrder> mutable = new java.util.ArrayList<>(Arrays.asList(SortOrder.asc("name")));

        // Act
        PageQuery query = PageQuery.of(0, 10, mutable);
        mutable.clear();

        // Assert
        assertEquals(1, query.getSort().size());
        assertThrows(UnsupportedOperationException.class,
                () -> query.getSort().add(SortOrder.desc("id")));
    }

    @Test
    @DisplayName("first page helper has no sorting and default size")
    void firstPage_hasDefaults() {
        // Act
        PageQuery query = PageQuery.firstPage();

        // Assert
        assertEquals(0, query.getPage());
        assertEquals(PageDefaults.DEFAULT_SIZE, query.getSize());
        assertTrue(query.getSort().isEmpty());
    }
}
