package hu.gov.nav.xsdparsertool.web.githubupdater.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Egy GitHub sémafrissítési futás összesített eredményét hordozó válaszobjektum, repository- és tag-szintű részletekkel.
 */
public class GitHubSchemaUpdateResponse {
    private Instant startedAt;
    private Instant finishedAt;
    private String organization;
    private String targetSchemaDir;
    private boolean dryRun;
    private int repositoryCount;
    private int downloadedCount;
    private int skippedCount;
    private int failedCount;
    private List<RepositoryUpdateResult> repositories = new ArrayList<>();

    /**
     * Visszaadja a(z) futás kezdési időpontja aktuális értékét.
     *
     * @return futás kezdési időpontja
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Beállítja a(z) futás kezdési időpontja értékét.
     *
     * @param startedAt a művelethez átadott {@code startedAt} érték
     */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * Visszaadja a(z) futás befejezési időpontja aktuális értékét.
     *
     * @return futás befejezési időpontja
     */
    public Instant getFinishedAt() {
        return finishedAt;
    }

    /**
     * Beállítja a(z) futás befejezési időpontja értékét.
     *
     * @param finishedAt a művelethez átadott {@code finishedAt} érték
     */
    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    /**
     * Visszaadja a(z) GitHub organization neve aktuális értékét.
     *
     * @return GitHub organization neve
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Beállítja a(z) GitHub organization neve értékét.
     *
     * @param organization a művelethez átadott {@code organization} érték
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * Visszaadja a(z) cél séma könyvtár aktuális értékét.
     *
     * @return cél séma könyvtár
     */
    public String getTargetSchemaDir() {
        return targetSchemaDir;
    }

    /**
     * Beállítja a(z) cél séma könyvtár értékét.
     *
     * @param targetSchemaDir a séma-release-ek alap célkönyvtára
     */
    public void setTargetSchemaDir(String targetSchemaDir) {
        this.targetSchemaDir = targetSchemaDir;
    }

    /**
     * Visszaadja a(z) dry-run jelző aktuális értékét.
     *
     * @return dry-run jelző
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Beállítja a(z) dry-run jelző értékét.
     *
     * @param dryRun a művelethez átadott {@code dryRun} érték
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Visszaadja a(z) repository-k száma aktuális értékét.
     *
     * @return repository-k száma
     */
    public int getRepositoryCount() {
        return repositoryCount;
    }

    /**
     * Beállítja a(z) repository-k száma értékét.
     *
     * @param repositoryCount az organization aktuális repository-száma
     */
    public void setRepositoryCount(int repositoryCount) {
        this.repositoryCount = repositoryCount;
    }

    /**
     * Visszaadja a(z) letöltött tételek száma aktuális értékét.
     *
     * @return letöltött tételek száma
     */
    public int getDownloadedCount() {
        return downloadedCount;
    }

    /**
     * Beállítja a(z) letöltött tételek száma értékét.
     *
     * @param downloadedCount a művelethez átadott {@code downloadedCount} érték
     */
    public void setDownloadedCount(int downloadedCount) {
        this.downloadedCount = downloadedCount;
    }

    /**
     * Visszaadja a(z) kihagyott tételek száma aktuális értékét.
     *
     * @return kihagyott tételek száma
     */
    public int getSkippedCount() {
        return skippedCount;
    }

    /**
     * Beállítja a(z) kihagyott tételek száma értékét.
     *
     * @param skippedCount a művelethez átadott {@code skippedCount} érték
     */
    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    /**
     * Visszaadja a(z) hibás tételek száma aktuális értékét.
     *
     * @return hibás tételek száma
     */
    public int getFailedCount() {
        return failedCount;
    }

    /**
     * Beállítja a(z) hibás tételek száma értékét.
     *
     * @param failedCount a művelethez átadott {@code failedCount} érték
     */
    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    /**
     * Visszaadja a(z) konfigurált repository-lista aktuális értékét.
     *
     * @return konfigurált repository-lista
     */
    public List<RepositoryUpdateResult> getRepositories() {
        return repositories;
    }

    /**
     * Beállítja a(z) konfigurált repository-lista értékét.
     *
     * @param repositories a művelethez átadott {@code repositories} érték
     */
    public void setRepositories(List<RepositoryUpdateResult> repositories) {
        this.repositories = repositories == null ? new ArrayList<>() : new ArrayList<>(repositories);
    }
}
