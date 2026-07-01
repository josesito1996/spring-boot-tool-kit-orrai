package com.library.support.orrai.http;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Framework-agnostic <b>super type token</b> that captures a full generic type (e.g.
 * {@code List<UserDto>}) which a plain {@code Class<R>} cannot express because of type erasure.
 *
 * <p>Lives in the Spring-free {@code core} so the {@link OrraiHttpClient} port can offer a generic
 * {@code execute} overload without depending on any HTTP library. Each adapter translates it to its
 * transport's own type reference (the {@code RestClient} adapter maps it to Spring's
 * {@code ParameterizedTypeReference}).
 *
 * <p>Create it by anonymous subclassing at the call site, so the type argument is retained in the
 * class's generic superclass:
 *
 * <pre>{@code
 * List<UserDto> users = http.execute(
 *         "/users", "GET", null, null, null, new OrraiTypeRef<List<UserDto>>() {});
 * }</pre>
 *
 * @param <T> the captured type
 */
public abstract class OrraiTypeRef<T> {

    private final Type type;

    protected OrraiTypeRef() {
        Type superClass = getClass().getGenericSuperclass();
        if (!(superClass instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "OrraiTypeRef must be created as an anonymous subclass carrying the actual type, "
                            + "e.g. new OrraiTypeRef<List<Foo>>() {}");
        }
        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    /** The captured generic {@link Type} (e.g. {@code java.util.List<UserDto>}). */
    public final Type getType() {
        return type;
    }
}