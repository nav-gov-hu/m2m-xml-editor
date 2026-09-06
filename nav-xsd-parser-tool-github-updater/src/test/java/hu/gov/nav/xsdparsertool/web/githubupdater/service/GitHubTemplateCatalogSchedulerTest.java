package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.Mockito.*;

class GitHubTemplateCatalogSchedulerTest {

    @Test
    void schedulerDoesNothingWhenUpdaterDisabled() {
        GitHubTemplateCatalogService service = mock(GitHubTemplateCatalogService.class);
        GitHubSchemaUpdaterProperties properties = configured();
        properties.setEnabled(false);

        new GitHubTemplateCatalogScheduler(service, properties).checkForChanges();

        verifyNoInteractions(service);
    }

    @Test
    void schedulerDoesNothingWithoutToken() {
        GitHubTemplateCatalogService service = mock(GitHubTemplateCatalogService.class);
        GitHubSchemaUpdaterProperties properties = configured();
        properties.setToken(null);

        new GitHubTemplateCatalogScheduler(service, properties).checkForChanges();

        verifyNoInteractions(service);
    }

    @Test
    void schedulerDoesNothingWhenIntervalIsDisabled() {
        GitHubTemplateCatalogService service = mock(GitHubTemplateCatalogService.class);
        GitHubSchemaUpdaterProperties properties = configured();
        properties.setCatalogCheckInterval(Duration.ZERO);

        new GitHubTemplateCatalogScheduler(service, properties).checkForChanges();

        verifyNoInteractions(service);
    }

    @Test
    void schedulerStartsBackgroundCheckWhenEnabledTokenizedAndScheduled() {
        GitHubTemplateCatalogService service = mock(GitHubTemplateCatalogService.class);
        GitHubSchemaUpdaterProperties properties = configured();

        new GitHubTemplateCatalogScheduler(service, properties).checkForChanges();

        verify(service).checkForChangesInBackground();
    }

    private GitHubSchemaUpdaterProperties configured() {
        GitHubSchemaUpdaterProperties properties = new GitHubSchemaUpdaterProperties();
        properties.setEnabled(true);
        properties.setToken("test-token");
        properties.setCatalogCheckInterval(Duration.ofMinutes(15));
        return properties;
    }
}
