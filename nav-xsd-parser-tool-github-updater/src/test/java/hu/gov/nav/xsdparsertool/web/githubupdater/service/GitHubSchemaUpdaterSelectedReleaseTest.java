package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubSchemaUpdaterSelectedReleaseTest {

    @Mock GitHubApiClient apiClient;
    @Mock Environment environment;
    @Mock ApplicationEventPublisher eventPublisher;
    @TempDir Path tempDir;

    @Test
    void dryRunForSelectedReleaseDoesNotFetchRepositoryTagList() throws Exception {
        GitHubSchemaUpdaterService service = service();
        GitHubSchemaUpdateRequest request = selected("NAV-2608", "v1.2.3");
        request.setDryRun(true);

        var response = service.updateSchemas(request);

        assertEquals(1, response.getRepositoryCount());
        assertEquals(0, response.getDownloadedCount());
        assertEquals(1, response.getSkippedCount());
        assertEquals("v1.2.3", response.getRepositories().get(0).getTags().get(0).getTagName());
        assertEquals("WOULD_DOWNLOAD", response.getRepositories().get(0).getTags().get(0).getStatus());
        verify(apiClient, never()).listRepositoryTags(anyString());
        verify(apiClient, never()).listOrganizationRepositories();
    }


    @Test
    void githubTechnicalRepositoryIsNeverDownloadedEvenWhenExplicitlySelected() {
        GitHubSchemaUpdaterService service = service();
        GitHubSchemaUpdateRequest request = selected(".github", "v1.0.0");

        var response = service.updateSchemas(request);

        assertEquals(0, response.getRepositoryCount());
        assertTrue(response.getRepositories().isEmpty());
        verifyNoInteractions(apiClient);
    }

    @Test
    void twoSelectedRepositoriesProduceTwoIndependentRepositoryResults() {
        GitHubSchemaUpdaterService service = service();
        GitHubSchemaUpdateRequest request = new GitHubSchemaUpdateRequest();
        request.setDryRun(true);
        LinkedHashMap<String, String> tags = new LinkedHashMap<>();
        tags.put("NAV-2608", "v1.0.0");
        tags.put("NAV-2609", "v2.0.0");
        request.setRepositoryTags(tags);

        var response = service.updateSchemas(request);

        assertEquals(2, response.getRepositoryCount());
        assertEquals(2, response.getRepositories().size());
        assertEquals("NAV-2608", response.getRepositories().get(0).getRepositoryName());
        assertEquals("NAV-2609", response.getRepositories().get(1).getRepositoryName());
        assertEquals(2, response.getSkippedCount());
    }

    @Test
    void commonRepositoryAlwaysTargetsLatestReleaseInsteadOfRequestedHistoricalTag() throws Exception {
        GitHubSchemaUpdaterService service = service();
        when(apiClient.listRepositoryTags("common")).thenReturn(java.util.List.of("v1.0.0", "v1.2.0", "v1.1.0"));
        GitHubSchemaUpdateRequest request = selected("common", "v1.0.0");
        request.setDryRun(true);

        var response = service.updateSchemas(request);

        assertEquals("v1.2.0", response.getRepositories().get(0).getTags().get(0).getTagName());
        assertEquals("WOULD_DOWNLOAD", response.getRepositories().get(0).getTags().get(0).getStatus());
        verify(apiClient).listRepositoryTags("common");
    }

    @Test
    void fullCheckCorePublicAlwaysTargetsLatestReleaseInsteadOfRequestedHistoricalTag() throws Exception {
        GitHubSchemaUpdaterService service = service();
        when(apiClient.listRepositoryTags("full_check_core_public"))
                .thenReturn(java.util.List.of("v3.1.0", "v3.3.0", "v3.2.0"));
        GitHubSchemaUpdateRequest request = selected("full_check_core_public", "v3.1.0");
        request.setDryRun(true);

        var response = service.updateSchemas(request);

        assertEquals("v3.3.0", response.getRepositories().get(0).getTags().get(0).getTagName());
        assertEquals("WOULD_DOWNLOAD", response.getRepositories().get(0).getTags().get(0).getStatus());
        assertEquals(tempDir.resolve("rule-xsl").toAbsolutePath().normalize().toString(),
                response.getRepositories().get(0).getTags().get(0).getTargetDirectory());
    }

    @Test
    void localFullCheckCorePublicPackageInstallsOnlyXslIntoConfiguredXslRoot() throws Exception {
        GitHubSchemaUpdaterService service = service();
        Path archive = tempDir.resolve("full_check_core_public-1.0.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("full_check_core_public-1.0/full_check_core_public.xsl"));
            zip.write("<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\"/>"
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("full_check_core_public-1.0/README.md"));
            zip.write("documentation".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        var result = service.importLocalArchive(archive, "full_check_core_public", "1.0", false);

        Path expectedTarget = tempDir.resolve("rule-xsl/full_check_core_public.xsl").toAbsolutePath().normalize();
        assertEquals("IMPORTED", result.getStatus());
        assertTrue(Files.isRegularFile(expectedTarget));
        assertFalse(Files.exists(tempDir.resolve("rule-xsl/README.md")));
        assertFalse(Files.exists(tempDir.resolve("xsd/full_check_core_public/1.0")));
        assertEquals(1, result.getInstalledArtifacts().size());
        assertEquals("XSL", result.getInstalledArtifacts().get(0).getArtifactType());
        assertEquals(expectedTarget.toString(), result.getInstalledArtifacts().get(0).getTargetFile());
    }


    @Test
    void forceLocalImportRemovesStaleInstalledFilesAndPublishesCacheRefreshEvent() throws Exception {
        GitHubSchemaUpdaterService service = service();
        Path staleDir = tempDir.resolve("xsd/NAV-2608/1.0");
        Files.createDirectories(staleDir);
        Files.writeString(staleDir.resolve("old-name.xsd"), "old");

        Path archive = tempDir.resolve("NAV-2608-1.0.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("NAV-2608-1.0/new-name.xsd"));
            zip.write("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>"
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        var result = service.importLocalArchive(archive, "NAV-2608", "1.0", true);

        assertEquals("IMPORTED", result.getStatus());
        assertFalse(Files.exists(staleDir.resolve("old-name.xsd")));
        assertTrue(Files.isRegularFile(staleDir.resolve("new-name.xsd")));
        verify(eventPublisher).publishEvent(any(hu.gov.nav.xsdparsertool.web.githubupdater.event.SchemaArtifactsChangedEvent.class));
    }

    private GitHubSchemaUpdaterService service() {
        GitHubSchemaUpdaterProperties properties = new GitHubSchemaUpdaterProperties();
        properties.setEnabled(true);
        properties.setTargetSchemaDir(tempDir.resolve("xsd"));
        lenient().when(environment.getProperty("nav.xsdparsertool.paths.common-xsd-dir")).thenReturn(tempDir.resolve("common").toString());
        lenient().when(environment.getProperty("nav.xsdparsertool.xpath-validator.rule-root-dir")).thenReturn(tempDir.resolve("rules").toString());
        lenient().when(environment.getProperty("nav.xsdparsertool.xpath-validator.xsl-root-dir")).thenReturn(tempDir.resolve("rule-xsl").toString());
        lenient().when(environment.getProperty("nav.xsdparsertool.paths.ui-model-dir")).thenReturn(tempDir.resolve("uimodel").toString());
        return new GitHubSchemaUpdaterService(properties, apiClient, new VersionTagComparator(), environment, eventPublisher);
    }

    private GitHubSchemaUpdateRequest selected(String repository, String tag) {
        GitHubSchemaUpdateRequest request = new GitHubSchemaUpdateRequest();
        request.setRepositoryTags(java.util.Map.of(repository, tag));
        return request;
    }
}
