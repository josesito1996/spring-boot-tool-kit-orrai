package com.library.support.orrai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Emits the structured "configuration loaded" confirmation once the context is ready. Reaching
 * {@link ApplicationReadyEvent} means Config Data already resolved the {@code configserver:} import
 * (with {@code fail-fast=true} an unreachable server would have halted startup), so this is a truthful
 * success line — not an optimistic guess. Secret values are never logged.
 */
public class ConfigClientStartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ConfigClientStartupLogger.class);

    private final ConfigClientProperties properties;

    public ConfigClientStartupLogger(ConfigClientProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Configuration successfully loaded from Config Server at {}.", properties.uri());
    }
}