package com.library.support.orrai.data.autoconfigure;

import com.library.support.orrai.data.audit.CurrentAuditor;
import com.library.support.orrai.data.audit.SystemAuditor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing for the Spring Boot 3 (jakarta) adapter so {@code AuditableEntity}
 * timestamps and {@code *By} columns are populated automatically.
 *
 * <p>Active by default; disable with {@code orrai.jpa.auditing.enabled=false} (for example when the
 * application already declares its own {@code @EnableJpaAuditing}). The {@code *By} value is
 * resolved through {@link CurrentAuditor}, defaulting to {@link SystemAuditor}; provide your own
 * {@code CurrentAuditor} bean (e.g. backed by Spring Security) to override it.
 */
@AutoConfiguration
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
@ConditionalOnProperty(prefix = "orrai.jpa.auditing", name = "enabled", matchIfMissing = true)
@EnableJpaAuditing(auditorAwareRef = "orraiAuditorAware")
public class OrraiJpaAuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CurrentAuditor currentAuditor() {
        return new SystemAuditor();
    }

    @Bean(name = "orraiAuditorAware")
    @ConditionalOnMissingBean(name = "orraiAuditorAware")
    public AuditorAware<String> orraiAuditorAware(CurrentAuditor currentAuditor) {
        return currentAuditor::currentAuditor;
    }
}
