package com.library.support.orrai.data.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageResponseTest {

    @Test
    @DisplayName("derives total pages by rounding up")
    void of_totalPages_roundsUp() {
        // Arrange
        PageQuery query = PageQuery.of(0, 10);

        // Act
        PageResponse<String> response = PageResponse.of(Arrays.asList("a", "b"), query, 25);

        // Assert
        assertEquals(3, response.getTotalPages());
        assertEquals(25, response.getTotalElements());
    }

    @Test
    @DisplayName("marks first and last flags for the opening page")
    void of_firstPage_flagsFirst() {
        // Arrange
        PageQuery query = PageQuery.of(0, 10);

        // Act
        PageResponse<String> response = PageResponse.of(Arrays.asList("a"), query, 5);

        // Assert
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        assertFalse(response.isEmpty());
    }

    @Test
    @DisplayName("marks last flag on the final page of several")
    void of_lastPage_flagsLast() {
        // Arrange
        PageQuery query = PageQuery.of(2, 10);

        // Act
        PageResponse<String> response = PageResponse.of(Arrays.asList("x"), query, 21);

        // Assert
        assertFalse(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    @DisplayName("reports empty when there is no content")
    void of_noContent_isEmpty() {
        // Arrange
        PageQuery query = PageQuery.of(0, 10);

        // Act
        PageResponse<String> response = PageResponse.of(Collections.emptyList(), query, 0);

        // Assert
        assertTrue(response.isEmpty());
        assertEquals(0, response.getTotalPages());
    }

    @Test
    @DisplayName("exposes content as an unmodifiable list")
    void of_content_isUnmodifiable() {
        // Arrange
        PageQuery query = PageQuery.of(0, 10);
        List<String> response = PageResponse.of(Arrays.asList("a"), query, 1).getContent();

        // Act + Assert
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> response.add("b"));
    }
}
