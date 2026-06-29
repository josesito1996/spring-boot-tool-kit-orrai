package com.library.support.orrai.data.datasource;

/**
 * Framework-agnostic connection-pool tuning, with sane production defaults. Plain mutable POJO so
 * the Spring Boot adapters can extend it with {@code @ConfigurationProperties("orrai.datasource")}
 * (relaxed binding binds inherited setters) without duplicating the fields, while this module stays
 * free of any Spring dependency.
 *
 * <p>Timeout fields are expressed in milliseconds to match HikariCP's API.
 */
public class DataSourcePoolProperties {

    /** JDBC URL. When unset, the adapter's opinionated DataSource auto-config stays inactive. */
    private String url;

    private String username;

    private String password;

    /** Maximum number of connections in the pool. */
    private int maximumPoolSize = 10;

    /** Minimum number of idle connections kept warm. */
    private int minimumIdle = 5;

    /** Maximum wait (ms) for a connection from the pool before failing. */
    private long connectionTimeoutMs = 30_000L;

    /** Maximum idle time (ms) before a connection is eligible for eviction. */
    private long idleTimeoutMs = 600_000L;

    /** Maximum lifetime (ms) of a connection in the pool. */
    private long maxLifetimeMs = 1_800_000L;

    /** Pool name, surfaced in metrics and logs. */
    private String poolName = "orrai-pool";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public void setMinimumIdle(int minimumIdle) {
        this.minimumIdle = minimumIdle;
    }

    public long getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public long getIdleTimeoutMs() {
        return idleTimeoutMs;
    }

    public void setIdleTimeoutMs(long idleTimeoutMs) {
        this.idleTimeoutMs = idleTimeoutMs;
    }

    public long getMaxLifetimeMs() {
        return maxLifetimeMs;
    }

    public void setMaxLifetimeMs(long maxLifetimeMs) {
        this.maxLifetimeMs = maxLifetimeMs;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }
}
