package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRelease;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateReleaseRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateRepositoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubTemplateCatalogPersistenceServiceTest {

    @Mock GitHubTemplateRepositoryRepository repositoryStore;
    @Mock GitHubTemplateReleaseRepository releaseStore;

    private GitHubTemplateCatalogPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new GitHubTemplateCatalogPersistenceService(repositoryStore, releaseStore);
    }

    @Test
    void missingRemoteReleaseIsPreservedUntilManualDeletion() {
        GitHubTemplateRepository repository = repository("NAV-OLD");
        GitHubTemplateRelease oldRelease = release("NAV-OLD", "1.0", false);
        when(repositoryStore.findById("NAV-OLD")).thenReturn(Optional.of(repository));
        when(releaseStore.findByRepositoryNameOrderByReleaseTagAsc("NAV-OLD"))
                .thenReturn(List.of(oldRelease));

        service.replaceRepositorySnapshot(
                new GitHubApiClient.RepositorySummary(
                        "NAV-OLD", "old", Instant.parse("2026-10-14T10:00:00Z"),
                        "https://example.invalid/NAV-OLD", false),
                List.of("2.0"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<GitHubTemplateRelease>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(releaseStore).saveAll(captor.capture());
        List<GitHubTemplateRelease> saved = new java.util.ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertEquals(2, saved.size());
        GitHubTemplateRelease preserved = saved.stream()
                .filter(item -> "1.0".equals(item.getReleaseTag()))
                .findFirst().orElseThrow();
        assertTrue(preserved.isLocalImported());
    }

    @Test
    void missingRepositoryIsMarkedArchivedWithoutDeletingReleases() {
        GitHubTemplateRepository repository = repository("NAV-OLD");
        when(repositoryStore.findById("NAV-OLD")).thenReturn(Optional.of(repository));

        service.markRepositoryMissingButPreserved("NAV-OLD");

        assertTrue(repository.isArchived());
        verify(repositoryStore).save(repository);
        verify(releaseStore, never()).deleteByRepositoryName(anyString());
    }

    private GitHubTemplateRepository repository(String name) {
        GitHubTemplateRepository repository = new GitHubTemplateRepository();
        repository.setRepositoryName(name);
        repository.setDescription(name);
        repository.setRepositoryUrl("https://example.invalid/" + name);
        repository.setArchived(false);
        repository.setLastSyncedAt(Instant.parse("2026-10-14T09:00:00Z"));
        return repository;
    }

    private GitHubTemplateRelease release(String repository, String tag, boolean localImported) {
        GitHubTemplateRelease release = new GitHubTemplateRelease();
        release.setRepositoryName(repository);
        release.setReleaseTag(tag);
        release.setLocalImported(localImported);
        release.setLastSyncedAt(Instant.parse("2026-10-14T09:00:00Z"));
        return release;
    }
}
