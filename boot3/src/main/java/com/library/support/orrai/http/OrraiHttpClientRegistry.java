package com.library.support.orrai.http;

import java.util.Map;
import java.util.Set;

/**
 * Holds every configured {@link OrraiHttpClient} keyed by name, so a consumer can talk to several
 * downstream services through one injection point:
 *
 * <pre>{@code
 * OrraiHttpClient payments = registry.client("payments");
 * }</pre>
 *
 * <p>The default client ({@code orrai.http.client.*}) is registered under {@link #DEFAULT_CLIENT}
 * and is also exposed as the {@code @Primary} {@link OrraiHttpClient} bean for direct injection.
 */
public class OrraiHttpClientRegistry {

    /** Registry key for the default client configured under {@code orrai.http.client.*}. */
    public static final String DEFAULT_CLIENT = "default";

    private final Map<String, OrraiHttpClient> clients;

    public OrraiHttpClientRegistry(Map<String, OrraiHttpClient> clients) {
        this.clients = Map.copyOf(clients);
    }

    /**
     * Returns the client registered under {@code name}.
     *
     * @throws IllegalArgumentException if no client is configured for that name
     */
    public OrraiHttpClient client(String name) {
        OrraiHttpClient client = clients.get(name);
        if (client == null) {
            throw new IllegalArgumentException(
                    "No OrraiHttpClient configured for name '" + name + "'. Configured clients: " + clients.keySet());
        }
        return client;
    }

    /** Whether a client is registered under {@code name}. */
    public boolean has(String name) {
        return clients.containsKey(name);
    }

    /** The names of all configured clients. */
    public Set<String> names() {
        return clients.keySet();
    }
}