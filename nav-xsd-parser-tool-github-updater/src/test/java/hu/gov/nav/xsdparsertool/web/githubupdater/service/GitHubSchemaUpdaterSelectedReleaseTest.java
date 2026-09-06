package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubSchemaUpdaterSelectedReleaseTest {

    @Mock GitHubApiClient apiClient;
    @Mock Environment environment;
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

    private GitHubSchemaUpdaterService service() {
        GitHubSchemaUpdaterProperties properties = new GitHubSchemaUpdaterProperties();
        properties.setEnabled(true);
        properties.setTargetSchemaDir(tempDir.resolve("xsd"));
        lenient().when(environment.getProperty("nav.xsdparsertool.paths.common-xsd-dir")).thenReturn(tempDir.resolve("common").toString());
        lenient().when(environment.getProperty("nav.xsdparsertool.xpath-validator.rule-root-dir")).thenReturn(tempDir.resolve("rules").toString());
        lenient().when(environment.getProperty("nav.xsdparsertool.xpath-validator.xsl-root-dir")).thenReturn(tempDir.resolve("rule-xsl").toString());
        return new GitHubSchemaUpdaterService(properties, apiClient, new VersionTagComparator(), environment);
    }

    private GitHubSchemaUpdateRequest selected(String repository, String tag) {
        GitHubSchemaUpdateRequest request = new GitHubSchemaUpdateRequest();
        request.setRepositoryTags(java.util.Map.of(repository, tag));
        return request;
    }
}
