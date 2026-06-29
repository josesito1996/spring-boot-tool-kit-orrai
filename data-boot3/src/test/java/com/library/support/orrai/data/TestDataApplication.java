package com.library.support.orrai.data;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot configuration so {@code @DataJpaTest} slices can bootstrap an application
 * context and entity-scan this base package. Test-only.
 */
@SpringBootApplication
public class TestDataApplication {
}
