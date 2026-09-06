package hu.gov.nav.xsdparsertool.web.githubupdater.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A GitHub Űrlapsablon-katalógus szervezetszintű szinkronizációs állapotát és időbélyegeit tároló perzisztens entitás.
 */
@Entity
@Table(name = "github_template_sync_state")
public class GitHubTemplateSyncState {
    @Id
    private Long id;
    @Column(name = "organization_name", length = 255, nullable = false)
    private String organizationName;
    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;
    @Column(name = "last_successful_sync_at")
    private Instant lastSuccessfulSyncAt;
    @Column(name = "repository_count", nullable = false)
    private int repositoryCount;

    /**
     * Visszaadja a(z) adatbázis-azonosító aktuális értékét.
     *
     * @return adatbázis-azonosító
     */
    public Long getId() { return id; }
    /**
     * Beállítja a(z) adatbázis-azonosító értékét.
     *
     * @param id a művelethez átadott {@code id} érték
     */
    public void setId(Long id) { this.id = id; }
    /**
     * Visszaadja a(z) organization neve aktuális értékét.
     *
     * @return organization neve
     */
    public String getOrganizationName() { return organizationName; }
    /**
     * Beállítja a(z) organization neve értékét.
     *
     * @param organizationName a művelethez átadott {@code organizationName} érték
     */
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    /**
     * Visszaadja a(z) utolsó ellenőrzés időpontja aktuális értékét.
     *
     * @return utolsó ellenőrzés időpontja
     */
    public Instant getLastCheckedAt() { return lastCheckedAt; }
    /**
     * Beállítja a(z) utolsó ellenőrzés időpontja értékét.
     *
     * @param lastCheckedAt a művelethez átadott {@code lastCheckedAt} érték
     */
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
    /**
     * Visszaadja a(z) utolsó sikeres szinkron időpontja aktuális értékét.
     *
     * @return utolsó sikeres szinkron időpontja
     */
    public Instant getLastSuccessfulSyncAt() { return lastSuccessfulSyncAt; }
    /**
     * Beállítja a(z) utolsó sikeres szinkron időpontja értékét.
     *
     * @param lastSuccessfulSyncAt a művelethez átadott {@code lastSuccessfulSyncAt} érték
     */
    public void setLastSuccessfulSyncAt(Instant lastSuccessfulSyncAt) { this.lastSuccessfulSyncAt = lastSuccessfulSyncAt; }
    /**
     * Visszaadja a(z) repository-k száma aktuális értékét.
     *
     * @return repository-k száma
     */
    public int getRepositoryCount() { return repositoryCount; }
    /**
     * Beállítja a(z) repository-k száma értékét.
     *
     * @param repositoryCount az organization aktuális repository-száma
     */
    public void setRepositoryCount(int repositoryCount) { this.repositoryCount = repositoryCount; }
}
