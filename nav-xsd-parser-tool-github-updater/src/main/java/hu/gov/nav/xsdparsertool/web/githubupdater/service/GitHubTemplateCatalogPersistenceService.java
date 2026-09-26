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

        List<GitHubTemplateRelease> localImports = releaseStore.findByRepositoryNameOrderByReleaseTagAsc(source.name()).stream()
                .filter(GitHubTemplateRelease::isLocalImported).toList();
        try {
            releaseStore.deleteByRepositoryName(source.name());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("A GitHub release-snapshot törlése sikertelen: " + source.name(), ex);
        }

        java.util.LinkedHashMap<String, GitHubTemplateRelease> merged = new java.util.LinkedHashMap<>();
        tags.stream().distinct().forEach(tag -> merged.put(tag, newRelease(source.name(), tag, now)));
        for (GitHubTemplateRelease local : localImports) {
            GitHubTemplateRelease release = merged.get(local.getReleaseTag());
            if (release == null) {
                GitHubTemplateRelease preserved = newRelease(source.name(), local.getReleaseTag(), now);
                preserved.setFormName(local.getFormName());
                preserved.setValidFrom(local.getValidFrom());
                preserved.setValidTo(local.getValidTo());
                preserved.setDisabled(local.isDisabled());
                preserved.setLocalImported(true);
                merged.put(local.getReleaseTag(), preserved);
            } else {
                release.setLocalImported(true);
            }
        }
        if (!merged.isEmpty()) releaseStore.saveAll(merged.values());
        releaseStore.flush();
    }

    /** A catalog repository egy űrlapbejegyzéséből teljes repository/release snapshotot ment. */
    @Transactional
    public void replaceCatalogSnapshot(ArtifactCatalogParser.FormEntry form, Instant generatedAt, String repositoryUrl) {
        Instant now = Instant.now();
        GitHubTemplateRepository target = RepositoryAccess.findById(repositoryStore, form.formId())
                .orElseGet(GitHubTemplateRepository::new);
        target.setRepositoryName(form.formId());
        target.setDescription(form.formName());
        target.setRepositoryUpdatedAt(generatedAt);
        target.setRepositoryUrl(repositoryUrl);
        target.setArchived(false);
        target.setLastSyncedAt(now);
        repositoryStore.save(target);
        List<GitHubTemplateRelease> localImports = releaseStore.findByRepositoryNameOrderByReleaseTagAsc(form.formId()).stream()
                .filter(GitHubTemplateRelease::isLocalImported).toList();
        releaseStore.deleteByRepositoryName(form.formId());
        java.util.LinkedHashMap<String, GitHubTemplateRelease> merged = new java.util.LinkedHashMap<>();
        form.versions().forEach(version -> {
            GitHubTemplateRelease release = newRelease(form.formId(), version.formVersion(), now);
            release.setFormName(form.formName());
            release.setValidFrom(version.validFrom());
            release.setValidTo(version.validTo());
            release.setDisabled(version.disabled());
            merged.put(version.formVersion(), release);
        });
        for (GitHubTemplateRelease local : localImports) {
            GitHubTemplateRelease release = merged.get(local.getReleaseTag());
            if (release == null) {
                GitHubTemplateRelease preserved = newRelease(form.formId(), local.getReleaseTag(), now);
                preserved.setFormName(local.getFormName());
                preserved.setValidFrom(local.getValidFrom());
                preserved.setValidTo(local.getValidTo());
                preserved.setDisabled(local.isDisabled());
                preserved.setLocalImported(true);
                merged.put(local.getReleaseTag(), preserved);
            } else {
                release.setLocalImported(true);
            }
        }
        if (!merged.isEmpty()) releaseStore.saveAll(merged.values());
        releaseStore.flush();
    }

    /** Lokális import után hiány esetén regisztrálja a repository/release párost, meglévő metaadat felülírása nélkül. */
    @Transactional
    public void registerLocalRelease(String repositoryName, String releaseTag, String repositoryUrl) {
        Instant now = Instant.now();
        GitHubTemplateRepository repository = RepositoryAccess.findById(repositoryStore, repositoryName).orElse(null);
        if (repository == null) {
            repository = new GitHubTemplateRepository();
            repository.setRepositoryName(repositoryName);
            repository.setDescription(repositoryName + " űrlapsablon");
            repository.setRepositoryUrl(repositoryUrl);
            repository.setArchived(false);
            repository.setLastSyncedAt(now);
            repositoryStore.save(repository);
        }
        GitHubTemplateRelease existing = releaseStore.findByRepositoryNameOrderByReleaseTagAsc(repositoryName).stream()
                .filter(release -> releaseTag.equals(release.getReleaseTag())).findFirst().orElse(null);
        if (existing == null) {
            existing = newRelease(repositoryName, releaseTag, now);
        }
        existing.setLocalImported(true);
        existing.setLastSyncedAt(now);
        releaseStore.save(existing);
        releaseStore.flush();
    }

    /** Egy konkrét release lokális regisztrációját törli; üres repository esetén a repository rekordot is eltávolítja. */
    @Transactional
    public void removeReleaseRegistration(String repositoryName, String releaseTag) {
        List<GitHubTemplateRelease> releases = releaseStore.findByRepositoryNameOrderByReleaseTagAsc(repositoryName);
        releases.stream()
                .filter(release -> releaseTag.equals(release.getReleaseTag()))
                .findFirst()
                .ifPresent(releaseStore::delete);
        releaseStore.flush();
        if (releaseStore.findByRepositoryNameOrderByReleaseTagAsc(repositoryName).isEmpty()) {
            repositoryStore.deleteById(repositoryName);
        }
    }

    /** Egy eltávolított repository release- és repository-rekordjait egy tranzakcióban törli. */
    @Transactional
    public void removeRepositorySnapshot(String repositoryName) {
        List<GitHubTemplateRelease> localImports = releaseStore.findByRepositoryNameOrderByReleaseTagAsc(repositoryName).stream()
                .filter(GitHubTemplateRelease::isLocalImported).toList();
        try {
            releaseStore.deleteByRepositoryName(repositoryName);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("A GitHub release-snapshot törlése sikertelen: " + repositoryName, ex);
        }
        if (localImports.isEmpty()) {
            repositoryStore.deleteById(repositoryName);
        } else {
            Instant now = Instant.now();
            List<GitHubTemplateRelease> preserved = localImports.stream().map(local -> {
                GitHubTemplateRelease copy = newRelease(repositoryName, local.getReleaseTag(), now);
                copy.setFormName(local.getFormName());
                copy.setValidFrom(local.getValidFrom());
                copy.setValidTo(local.getValidTo());
                copy.setDisabled(local.isDisabled());
                copy.setLocalImported(true);
                return copy;
            }).toList();
            releaseStore.saveAll(preserved);
            releaseStore.flush();
        }
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
