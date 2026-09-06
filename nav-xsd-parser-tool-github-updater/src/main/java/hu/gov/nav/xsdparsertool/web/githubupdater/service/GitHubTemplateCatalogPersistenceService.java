package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.web.githubupdater.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRelease;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateReleaseRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateRepositoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * A GitHub template katalógus repository- és release-snapshotjainak tranzakciós mentése.
 *
 * <p>Külön Spring bean szükséges, mert a katalógusfrissítés aszinkron szálon fut, és az
 * azonos osztályon belüli önhívás nem aktiválja a Spring {@code @Transactional} proxyját.</p>
 */
@Service
public class GitHubTemplateCatalogPersistenceService {

    private final GitHubTemplateRepositoryRepository repositoryStore;
    private final GitHubTemplateReleaseRepository releaseStore;

    /**
     * Létrehozza a(z) {@code GitHubTemplateCatalogPersistenceService} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param repositoryStore a művelethez átadott {@code repositoryStore} érték
     * @param releaseStore a művelethez átadott {@code releaseStore} érték
     */
    public GitHubTemplateCatalogPersistenceService(GitHubTemplateRepositoryRepository repositoryStore,
                                                     GitHubTemplateReleaseRepository releaseStore) {
        this.repositoryStore = repositoryStore;
        this.releaseStore = releaseStore;
    }

    /**
     * Egy repository teljes release-snapshotját atomikusan lecseréli.
     * Hiba esetén a korábbi release-lista visszaáll, így nem marad félkész vagy üres állapot.
     */
    @Transactional
    public void replaceRepositorySnapshot(GitHubApiClient.RepositorySummary source, List<String> tags) {
        Instant now = Instant.now();

        GitHubTemplateRepository target = RepositoryAccess.findById(repositoryStore, source.name())
                .orElseGet(GitHubTemplateRepository::new);
        target.setRepositoryName(source.name());
        target.setDescription(source.description());
        target.setRepositoryUpdatedAt(source.updatedAt());
        target.setRepositoryUrl(source.htmlUrl());
        target.setArchived(source.archived());
        target.setLastSyncedAt(now);
        repositoryStore.save(target);

        try {
            releaseStore.deleteByRepositoryName(source.name());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("A GitHub release-snapshot törlése sikertelen: " + source.name(), ex);
        }

        List<GitHubTemplateRelease> releases = tags.stream()
                .distinct()
                .map(tag -> newRelease(source.name(), tag, now))
                .toList();
        if (!releases.isEmpty()) {
            releaseStore.saveAll(releases);
        }
        releaseStore.flush();
    }

    /** Egy eltávolított repository release- és repository-rekordjait egy tranzakcióban törli. */
    @Transactional
    public void removeRepositorySnapshot(String repositoryName) {
        try {
            releaseStore.deleteByRepositoryName(repositoryName);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("A GitHub release-snapshot törlése sikertelen: " + repositoryName, ex);
        }
        repositoryStore.deleteById(repositoryName);
    }

    /**
     * Új katalógus release-entitást állít össze a repository névből, release tagből és az egységes szinkronidőpontból.
     *
     * @param repositoryName a GitHub repository neve
     * @param releaseTag a művelethez átadott {@code releaseTag} érték
     * @param syncedAt a művelethez átadott {@code syncedAt} érték
     * @return a művelet eredménye
     */
    private GitHubTemplateRelease newRelease(String repositoryName, String releaseTag, Instant syncedAt) {
        GitHubTemplateRelease release = new GitHubTemplateRelease();
        release.setRepositoryName(repositoryName);
        release.setReleaseTag(releaseTag);
        release.setLastSyncedAt(syncedAt);
        return release;
    }
}
