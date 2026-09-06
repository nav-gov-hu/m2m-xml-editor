package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A GitHub Űrlapsablon-katalógus periodikus, háttérben végzett változásellenőrzését indító Spring komponens.
 */
@Component
public class GitHubTemplateCatalogScheduler {
    private final GitHubTemplateCatalogService service;
    private final GitHubSchemaUpdaterProperties properties;

    /**
     * Létrehozza a(z) {@code GitHubTemplateCatalogScheduler} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param service a művelethez átadott {@code service} érték
     * @param properties a művelethez átadott {@code properties} érték
     */
    public GitHubTemplateCatalogScheduler(GitHubTemplateCatalogService service,
                                          GitHubSchemaUpdaterProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    /**
     * Ellenőrzi az aktuális GitHub/katalógus állapotot, és a vizsgálat eredményét a hívó számára elérhető állapotba rendezi.
     */
    @Scheduled(
            initialDelayString = "${nav.xsdparsertool.github-schema-updater.catalog-check-initial-delay:PT1M}",
            fixedDelayString = "${nav.xsdparsertool.github-schema-updater.catalog-check-interval:PT15M}")
    public void checkForChanges() {
        if (!properties.isEnabled() || !properties.hasToken() || properties.getCatalogCheckInterval() == null
                || properties.getCatalogCheckInterval().isZero() || properties.getCatalogCheckInterval().isNegative()) {
            return;
        }
        service.checkForChangesInBackground();
    }
}
