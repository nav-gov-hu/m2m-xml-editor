package hu.gov.nav.xsdparsertool.web.githubupdater.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * A GitHub sémafrissítő Spring konfigurációs belépési pontja, amely engedélyezi a {@link GitHubSchemaUpdaterProperties} property-k kötését.
 */
@Configuration
@EnableConfigurationProperties(GitHubSchemaUpdaterProperties.class)
public class GitHubSchemaUpdaterConfiguration {
}
