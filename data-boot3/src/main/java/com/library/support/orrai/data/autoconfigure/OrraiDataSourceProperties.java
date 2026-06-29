package com.library.support.orrai.data.autoconfigure;

import com.library.support.orrai.data.datasource.DataSourcePoolProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code orrai.datasource.*} onto the shared {@link DataSourcePoolProperties} (relaxed binding
 * binds the inherited setters), so the pool fields live in one place in {@code data-core}.
 */
@ConfigurationProperties(prefix = "orrai.datasource")
public class OrraiDataSourceProperties extends DataSourcePoolProperties {
}
