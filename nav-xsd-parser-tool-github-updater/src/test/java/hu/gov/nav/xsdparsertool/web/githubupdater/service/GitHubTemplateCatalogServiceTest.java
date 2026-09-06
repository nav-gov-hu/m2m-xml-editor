package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.githubupdater.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRelease;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateSyncState;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateResponse;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubTemplateCatalogDtos;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateReleaseRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateRepositoryRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateSyncStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubTemplateCatalogServiceTest {

    @Mock GitHubApiClient apiClient;
    @Mock GitHubSchemaUpdaterService updaterService;
    @Mock Environment environment;
    @Mock GitHubTemplateRepositoryRepository repositoryStore;
    @Mock GitHubTemplateReleaseRepository releaseStore;
    @Mock GitHubTemplateSyncStateRepository syncStateStore;
    @Mock GitHubTemplateCatalogPersistenceService persistenceService;

    @TempDir Path tempDir;

    private GitHubSchemaUpdaterProperties properties;
    private GitHubTemplateCatalogService service;

    @BeforeEach
    void setUp() {
        properties = new GitHubSchemaUpdaterProperties();
        properties.setOrganization("nav-test");
        service = new GitHubTemplateCatalogService(
                apiClient,
                updaterService,
                properties,
                new VersionTagComparator(),
                environment,
                repositoryStore,
                releaseStore,
                syncStateStore,
                persistenceService);
    }

    @Test
    void catalogWithoutTokenUsesOnlyStoredCatalogAndDoesNotCallGitHub() {
        GitHubTemplateRepository repository = repository("NAV-2608", "2608 form");
        GitHubTemplateRelease release = release("NAV-2608", "v1.2.3");
        when(RepositoryAccess.findAll(repositoryStore)).thenReturn(List.of(repository));
        when(releaseStore.findByRepositoryNameOrderByReleaseTagAsc("NAV-2608")).thenReturn(List.of(release));
        when(RepositoryAccess.findById(syncStateStore, 1L)).thenReturn(Optional.empty());
        when(environment.getProperty("nav.xsdparsertool.paths.schema-dir")).thenReturn(tempDir.toString());
        when(updaterService.resolveTargetSchemaDir()).thenReturn(tempDir);

        GitHubTemplateCatalogDtos.CatalogResponse response = service.catalog(false);

        assertFalse(response.tokenConfigured());
        assertEquals("nav-test", response.organization());
        assertEquals(1, response.repositoryCount());
        assertEquals(1, response.rowCount());
        assertEquals("NAV-2608", response.rows().get(0).repository());
        verifyNoInteractions(apiClient);
    }

    @Test
    void checkForChangesWithoutTokenFailsBeforeGitHubCall() {
        IllegalStateException error = assertThrows(IllegalStateException.class, service::checkForChanges);

        assertTrue(error.getMessage().contains("token"));
        verifyNoInteractions(apiClient);
    }

    @Test
    void changeCheckDetectsCatalogDeltaWithoutStartingReleaseDownload() throws Exception {
        properties.setToken("test-token");
        GitHubTemplateRepository existing = repository("NAV-OLD", "old");
        when(RepositoryAccess.findAll(repositoryStore)).thenReturn(List.of(existing));
        when(apiClient.listOrganizationRepositorySummaries()).thenReturn(List.of(
                new GitHubApiClient.RepositorySummary(
                        "NAV-NEW", "new", Instant.parse("2026-08-02T10:00:00Z"),
                        "https://example.invalid/NAV-NEW", false)));
        when(RepositoryAccess.findById(syncStateStore, 1L)).thenReturn(Optional.empty());

        GitHubTemplateCatalogDtos.ChangeCheckResponse response = service.checkForChanges();

        assertTrue(response.changesDetected());
        assertEquals(List.of("NAV-NEW"), response.changedRepositories());
        assertEquals(List.of("NAV-OLD"), response.removedRepositories());
        verify(updaterService, never()).updateSchemas(any());
        verify(syncStateStore).save(any(GitHubTemplateSyncState.class));
    }

    @Test
    void startRefreshWithoutTokenIsRejectedWithoutStartingRemoteWork() {
        GitHubTemplateCatalogDtos.RefreshStartResponse response = service.startRefresh();

        assertFalse(response.started());
        assertTrue(response.message().contains("token"));
        verifyNoInteractions(apiClient);
    }

    @Test
    void downloadWithoutTokenIsRejectedBeforeUpdaterInvocation() {
        GitHubTemplateCatalogDtos.DownloadRequest request = new GitHubTemplateCatalogDtos.DownloadRequest(
                List.of(new GitHubTemplateCatalogDtos.DownloadItem("NAV-2608", "v1.2.3")), false);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.download(request));

        assertTrue(error.getMessage().contains("token"));
        verifyNoInteractions(updaterService);
    }

    @Test
    void selectedReleaseIsDelegatedExactlyToUpdater() {
        properties.setToken("test-token");
        GitHubSchemaUpdateResponse expected = new GitHubSchemaUpdateResponse();
        when(updaterService.updateSchemas(any())).thenReturn(expected);
        GitHubTemplateCatalogDtos.DownloadRequest request = new GitHubTemplateCatalogDtos.DownloadRequest(
                List.of(new GitHubTemplateCatalogDtos.DownloadItem("NAV-2608", "v1.2.3")), true);

        GitHubSchemaUpdateResponse actual = service.download(request);

        assertSame(expected, actual);
        var captor = org.mockito.ArgumentCaptor.forClass(
                hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateRequest.class);
        verify(updaterService).updateSchemas(captor.capture());
        assertEquals("v1.2.3", captor.getValue().getRepositoryTags().get("NAV-2608"));
        assertTrue(captor.getValue().isForceDownloadAll());
    }

    @Test
    void blankSelectionsAreRejected() {
        properties.setToken("test-token");
        GitHubTemplateCatalogDtos.DownloadRequest request = new GitHubTemplateCatalogDtos.DownloadRequest(
                List.of(new GitHubTemplateCatalogDtos.DownloadItem(" ", " ")), false);

        assertThrows(IllegalArgumentException.class, () -> service.download(request));
        verifyNoInteractions(updaterService);
    }

    @Test
    void normalReleaseIsReportedLocalOnlyWhenRepositoryTagDirectoryExists() throws Exception {
        properties.setToken("test-token");
        Path releaseDir = tempDir.resolve("NAV-2608").resolve("v1.2.3");
        ExceptionSafeOperations.createDirectories(releaseDir);
        when(updaterService.resolveTargetSchemaDir()).thenReturn(tempDir);
        when(RepositoryAccess.findAll(repositoryStore)).thenReturn(List.of(repository("NAV-2608", "2608 form")));
        when(releaseStore.findByRepositoryNameOrderByReleaseTagAsc("NAV-2608"))
                .thenReturn(List.of(release("NAV-2608", "v1.2.3")));
        when(RepositoryAccess.findById(syncStateStore, 1L)).thenReturn(Optional.empty());
        when(environment.getProperty("nav.xsdparsertool.paths.schema-dir")).thenReturn(tempDir.toString());

        GitHubTemplateCatalogDtos.CatalogResponse response = service.catalog(false);

        assertTrue(response.rows().get(0).locallyAvailable());
        verifyNoInteractions(apiClient);
    }

    @Test
    void commonRepositoryUsesDirectActiveDirectoryForLocalAvailability() throws Exception {
        Path commonDir = Files.createDirectory(tempDir.resolve("common"));
        Files.writeString(commonDir.resolve("common.xsd"), "<schema/>");
        when(environment.getProperty("nav.xsdparsertool.paths.common-xsd-dir")).thenReturn(commonDir.toString());
        when(environment.getProperty("nav.xsdparsertool.paths.schema-dir")).thenReturn(tempDir.toString());
        when(RepositoryAccess.findAll(repositoryStore)).thenReturn(List.of(repository("common", "common")));
        when(releaseStore.findByRepositoryNameOrderByReleaseTagAsc("common"))
                .thenReturn(List.of(release("common", "v9.9.9")));
        when(RepositoryAccess.findById(syncStateStore, 1L)).thenReturn(Optional.empty());

        GitHubTemplateCatalogDtos.CatalogResponse response = service.catalog(false);

        assertTrue(response.rows().get(0).locallyAvailable());
        verify(updaterService, never()).resolveTargetSchemaDir();
        verifyNoInteractions(apiClient);
    }

    @Test
    void fullCheckCorePublicUsesDirectActiveDirectoryForLocalAvailability() throws Exception {
        Path xslDir = Files.createDirectory(tempDir.resolve("rule-xsl"));
        Files.writeString(xslDir.resolve("full_check_core_public.xsl"), "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\"/>");
        when(environment.getProperty("nav.xsdparsertool.xpath-validator.xsl-root-dir")).thenReturn(xslDir.toString());
        when(environment.getProperty("nav.xsdparsertool.paths.schema-dir")).thenReturn(tempDir.toString());
        when(RepositoryAccess.findAll(repositoryStore)).thenReturn(List.of(repository("full_check_core_public", "rules")));
        when(releaseStore.findByRepositoryNameOrderByReleaseTagAsc("full_check_core_public"))
                .thenReturn(List.of(release("full_check_core_public", "v3.0.0")));
        when(RepositoryAccess.findById(syncStateStore, 1L)).thenReturn(Optional.empty());

        GitHubTemplateCatalogDtos.CatalogResponse response = service.catalog(false);

        assertTrue(response.rows().get(0).locallyAvailable());
        verify(updaterService, never()).resolveTargetSchemaDir();
    }

    @Test
    void localBundleContainsOnlyLocalFilesAndDoesNotCallGitHub() throws Exception {
        Path schemaRoot = Files.createDirectory(tempDir.resolve("xsd"));
        Path commonRoot = Files.createDirectory(tempDir.resolve("common"));
        Path uiRoot = Files.createDirectory(tempDir.resolve("ui"));
        Path xpathRoot = Files.createDirectory(tempDir.resolve("xpath"));
        Files.writeString(commonRoot.resolve("common.xsd"), "common");
        Path formSchema = ExceptionSafeOperations.createDirectories(schemaRoot.resolve("NAV-2608").resolve("1.2.3"));
        Files.writeString(formSchema.resolve("form.xsd"), "xsd");
        Path formUi = ExceptionSafeOperations.createDirectories(uiRoot.resolve("NAV-2608").resolve("1.2.3"));
        Files.writeString(formUi.resolve("ui.xml"), "ui");
        Path formXpath = ExceptionSafeOperations.createDirectories(xpathRoot.resolve("NAV-2608").resolve("1.2.3"));
        Files.writeString(formXpath.resolve("rule.xsl"), "xsl");
        when(environment.getProperty("nav.xsdparsertool.paths.schema-dir")).thenReturn(schemaRoot.toString());
        when(environment.getProperty("nav.xsdparsertool.paths.common-xsd-dir")).thenReturn(commonRoot.toString());
        when(environment.getProperty("nav.xsdparsertool.paths.ui-model-dir")).thenReturn(uiRoot.toString());
        when(environment.getProperty("nav.xsdparsertool.xpath-validator.rule-root-dir")).thenReturn(xpathRoot.toString());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.writeLocalBundle("NAV-2608", "v1.2.3", output);

        var names = new java.util.HashSet<String>();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(output.toByteArray()))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        assertEquals(java.util.Set.of("common.xsd", "form.xsd", "ui.xml", "rule.xsl"), names);
        verifyNoInteractions(apiClient);
    }

    @Test
    void localBundleRejectsDotPathSegmentsBeforeReadingConfiguredRoots() {
        assertThrows(IllegalArgumentException.class,
                () -> service.writeLocalBundle(".", "v1.2.3", new ByteArrayOutputStream()));
        assertThrows(IllegalArgumentException.class,
                () -> service.writeLocalBundle("NAV-2608", "release/..", new ByteArrayOutputStream()));
        assertThrows(IllegalArgumentException.class,
                () -> service.localBundleFileName(".", "v1.2.3"));
    }

    private GitHubTemplateRepository repository(String name, String description) {
        GitHubTemplateRepository repository = new GitHubTemplateRepository();
        repository.setRepositoryName(name);
        repository.setDescription(description);
        repository.setRepositoryUpdatedAt(Instant.parse("2026-08-01T10:00:00Z"));
        repository.setRepositoryUrl("https://example.invalid/" + name);
        repository.setLastSyncedAt(Instant.parse("2026-08-01T10:05:00Z"));
        return repository;
    }

    private GitHubTemplateRelease release(String repositoryName, String tag) {
        GitHubTemplateRelease release = new GitHubTemplateRelease();
        release.setRepositoryName(repositoryName);
        release.setReleaseTag(tag);
        release.setLastSyncedAt(Instant.parse("2026-08-01T10:05:00Z"));
        return release;
    }
}
