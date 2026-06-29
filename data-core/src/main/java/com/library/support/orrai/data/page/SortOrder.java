package com.library.support.orrai.data.page;

import java.util.Objects;

/**
 * A single sort instruction: a property name plus a {@link Direction}. Immutable value type
 * (plain class, not a {@code record}, to keep the Java 11 floor shared with the {@code core}
 * module).
 */
public final class SortOrder {

    private final String property;

    private final Direction direction;

    public SortOrder(String property, Direction direction) {
        if (property == null || property.trim().isEmpty()) {
            throw new IllegalArgumentException("Sort property must not be blank");
        }
        this.property = property;
        this.direction = direction == null ? Direction.ASC : direction;
    }

    /** Ascending sort on the given property. */
    public static SortOrder asc(String property) {
        return new SortOrder(property, Direction.ASC);
    }

    /** Descending sort on the given property. */
    public static SortOrder desc(String property) {
        return new SortOrder(property, Direction.DESC);
    }

    public String getProperty() {
        return property;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SortOrder)) {
            return false;
        }
        SortOrder that = (SortOrder) o;
        return property.equals(that.property) && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, direction);
    }

    @Override
    public String toString() {
        return property + ": " + direction;
    }
}
