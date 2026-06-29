package com.library.support.orrai.data.autoconfigure;

import com.library.support.orrai.data.datasource.DataSourcePoolProperties;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Opt-in opinionated {@link DataSource} for the Spring Boot 2 adapter. Active only when
 * {@code orrai.datasource.url} is set, so it never collides with Spring Boot's own DataSource
 * auto-configuration driven by {@code spring.datasource.*}; when both are configured, the
 * application's existing {@code DataSource} bean wins ({@code @ConditionalOnMissingBean}).
 *
 * <p>The connection-pool fields come from {@link OrraiDataSourceProperties}
 * ({@code orrai.datasource.*}), giving production-ready Hikari defaults out of the box.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HikariDataSource.class)
@ConditionalOnProperty(prefix = "orrai.datasource", name = "url")
@EnableConfigurationProperties(OrraiDataSourceProperties.class)
public class OrraiDataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource orraiDataSource(OrraiDataSourceProperties properties) {
        return buildHikari(properties);
    }

    static HikariDataSource buildHikari(DataSourcePoolProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setPoolName(properties.getPoolName());
        dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        dataSource.setMinimumIdle(properties.getMinimumIdle());
        dataSource.setConnectionTimeout(properties.getConnectionTimeoutMs());
        dataSource.setIdleTimeout(properties.getIdleTimeoutMs());
        dataSource.setMaxLifetime(properties.getMaxLifetimeMs());
        return dataSource;
    }
}
