package com.library.support.orrai.config;

/**
 * Thrown at startup when the Config Client starter is enabled but its configuration is invalid
 * (e.g. a missing {@code starter.config.uri} or a half-specified basic-auth pair). Extends
 * {@link IllegalStateException} so it fails the environment post-processing fast with a clear,
 * actionable message instead of surfacing later as a cryptic Config Data error.
 */
public class ConfigClientConfigurationException extends IllegalStateException {

    public ConfigClientConfigurationException(String message) {
        super(message);
    }
}