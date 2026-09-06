package hu.gov.nav.xsdparsertool.web.githubupdater.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A GitHub sémafrissítés paramétereit hordozó kérésobjektum. Repository-listával, repository→tag kiválasztással, dry-run és kényszerített újraletöltési jelzővel szűkíthető a művelet.
 */
public class GitHubSchemaUpdateRequest {
    private boolean dryRun;
    private boolean forceDownloadAll;
    private List<String> repositories = new ArrayList<>();
    private Map<String, String> repositoryTags = new LinkedHashMap<>();

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
     * Visszaadja a(z) kényszerített újraletöltés jelző aktuális értékét.
     *
     * @return kényszerített újraletöltés jelző
     */
    public boolean isForceDownloadAll() {
        return forceDownloadAll;
    }

    /**
     * Beállítja a(z) kényszerített újraletöltés jelző értékét.
     *
     * @param forceDownloadAll a művelethez átadott {@code forceDownloadAll} érték
     */
    public void setForceDownloadAll(boolean forceDownloadAll) {
        this.forceDownloadAll = forceDownloadAll;
    }

    /**
     * Visszaadja a(z) konfigurált repository-lista aktuális értékét.
     *
     * @return konfigurált repository-lista
     */
    public List<String> getRepositories() {
        return repositories;
    }

    /**
     * Beállítja a(z) konfigurált repository-lista értékét.
     *
     * @param repositories a művelethez átadott {@code repositories} érték
     */
    public void setRepositories(List<String> repositories) {
        this.repositories = repositories == null ? new ArrayList<>() : new ArrayList<>(repositories);
    }
    /**
     * Visszaadja a(z) repository és kiválasztott tag párok aktuális értékét.
     *
     * @return repository és kiválasztott tag párok
     */
    public Map<String, String> getRepositoryTags() { return repositoryTags; }
    /**
     * Beállítja a(z) repository és kiválasztott tag párok értékét.
     *
     * @param repositoryTags a művelethez átadott {@code repositoryTags} érték
     */
    public void setRepositoryTags(Map<String, String> repositoryTags) {
        this.repositoryTags = repositoryTags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(repositoryTags);
    }
}

