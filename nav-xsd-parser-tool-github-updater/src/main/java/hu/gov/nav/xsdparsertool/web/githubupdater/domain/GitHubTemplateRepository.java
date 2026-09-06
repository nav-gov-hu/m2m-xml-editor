package hu.gov.nav.xsdparsertool.web.githubupdater.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A GitHub Űrlapsablon-katalógus egy repository-bejegyzését és annak utolsó ismert távoli metaadatait reprezentáló perzisztens entitás.
 */
@Entity
@Table(name = "github_template_repository")
public class GitHubTemplateRepository {
    @Id
    @Column(name = "repository_name", length = 255, nullable = false)
    private String repositoryName;
    @Column(name = "description_text", length = 2000)
    private String description;
    @Column(name = "repository_updated_at")
    private Instant repositoryUpdatedAt;
    @Column(name = "repository_url", length = 1000)
    private String repositoryUrl;
    @Column(name = "archived_flag", nullable = false)
    private boolean archived;
    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    /**
     * Visszaadja a(z) repository neve aktuális értékét.
     *
     * @return repository neve
     */
    public String getRepositoryName() { return repositoryName; }
    /**
     * Beállítja a(z) repository neve értékét.
     *
     * @param repositoryName a GitHub repository neve
     */
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
    /**
     * Visszaadja a(z) repository leírása aktuális értékét.
     *
     * @return repository leírása
     */
    public String getDescription() { return description; }
    /**
     * Beállítja a(z) repository leírása értékét.
     *
     * @param description a művelethez átadott {@code description} érték
     */
    public void setDescription(String description) { this.description = description; }
    /**
     * Visszaadja a(z) távoli repository utolsó módosítási ideje aktuális értékét.
     *
     * @return távoli repository utolsó módosítási ideje
     */
    public Instant getRepositoryUpdatedAt() { return repositoryUpdatedAt; }
    /**
     * Beállítja a(z) távoli repository utolsó módosítási ideje értékét.
     *
     * @param repositoryUpdatedAt a művelethez átadott {@code repositoryUpdatedAt} érték
     */
    public void setRepositoryUpdatedAt(Instant repositoryUpdatedAt) { this.repositoryUpdatedAt = repositoryUpdatedAt; }
    /**
     * Visszaadja a(z) repository webes URL-je aktuális értékét.
     *
     * @return repository webes URL-je
     */
    public String getRepositoryUrl() { return repositoryUrl; }
    /**
     * Beállítja a(z) repository webes URL-je értékét.
     *
     * @param repositoryUrl a művelethez átadott {@code repositoryUrl} érték
     */
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    /**
     * Visszaadja a(z) archivált állapot aktuális értékét.
     *
     * @return archivált állapot
     */
    public boolean isArchived() { return archived; }
    /**
     * Beállítja a(z) archivált állapot értékét.
     *
     * @param archived a művelethez átadott {@code archived} érték
     */
    public void setArchived(boolean archived) { this.archived = archived; }
    /**
     * Visszaadja a(z) utolsó szinkron időpontja aktuális értékét.
     *
     * @return utolsó szinkron időpontja
     */
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    /**
     * Beállítja a(z) utolsó szinkron időpontja értékét.
     *
     * @param lastSyncedAt a művelethez átadott {@code lastSyncedAt} érték
     */
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
