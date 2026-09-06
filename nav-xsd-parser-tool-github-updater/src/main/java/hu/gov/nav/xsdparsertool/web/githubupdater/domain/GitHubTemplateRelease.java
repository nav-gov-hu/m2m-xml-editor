package hu.gov.nav.xsdparsertool.web.githubupdater.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Egy GitHub Űrlapsablon repository lokális katalógusban nyilvántartott release-tagjét reprezentáló perzisztens entitás.
 */
@Entity
@Table(name = "github_template_release", uniqueConstraints = @UniqueConstraint(name = "uk_github_template_release_repo_tag", columnNames = {"repository_name", "release_tag"}))
public class GitHubTemplateRelease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "repository_name", length = 255, nullable = false)
    private String repositoryName;
    @Column(name = "release_tag", length = 255, nullable = false)
    private String releaseTag;
    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    /**
     * Visszaadja a(z) adatbázis-azonosító aktuális értékét.
     *
     * @return adatbázis-azonosító
     */
    public Long getId() { return id; }
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
     * Visszaadja a(z) release tag aktuális értékét.
     *
     * @return release tag
     */
    public String getReleaseTag() { return releaseTag; }
    /**
     * Beállítja a(z) release tag értékét.
     *
     * @param releaseTag a művelethez átadott {@code releaseTag} érték
     */
    public void setReleaseTag(String releaseTag) { this.releaseTag = releaseTag; }
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
