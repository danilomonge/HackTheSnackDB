package de.pecus.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration.
 *
 * The DataSource (HikariCP), EntityManagerFactory and TransactionManager are
 * created by Spring Boot's auto-configuration from the {@code spring.datasource.*}
 * and {@code spring.jpa.*} properties in application.yml. HikariCP validates
 * connections before handing them out, so connections dropped by the database
 * or its pooler while the service was idle are replaced transparently.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "de.pecus.api.repositories")
public class PersistenceConfig {
}
