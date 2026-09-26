package hu.gov.nav.xsdparsertool.web.githubupdater.config;

/**
 * Meghatározza, hogy az űrlapsablon-katalógus változásait a hagyományos GitHub REST API
 * vagy a dedikált catalog repository artifact-catalog.xml állománya alapján vizsgáljuk.
 */
public enum GitHubCatalogSourceMode {
    GITHUB_API,
    CATALOG
}
