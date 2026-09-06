package hu.gov.nav.xsdparsertool.web.githubupdater.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Egy konkrét GitHub release-tag feldolgozásának állapotát, célkönyvtárát és telepített artefaktumait reprezentáló DTO.
 */
public class TagUpdateResult {
    private String tagName;
    private String status;
    private String targetDirectory;
    private String message;
    private List<InstalledArtifactResult> installedArtifacts = new ArrayList<>();

    /**
     * Létrehozza a(z) {@code TagUpdateResult} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     */
    public TagUpdateResult() {
    }

    /**
     * Létrehozza a(z) {@code TagUpdateResult} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param tagName a release tag neve
     * @param status a művelethez átadott {@code status} érték
     * @param targetDirectory a célkönyvtár
     * @param message a művelethez átadott {@code message} érték
     */
    public TagUpdateResult(String tagName, String status, String targetDirectory, String message) {
        this.tagName = tagName;
        this.status = status;
        this.targetDirectory = targetDirectory;
        this.message = message;
    }

    /**
     * Visszaadja a(z) tag neve aktuális értékét.
     *
     * @return tag neve
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * Beállítja a(z) tag neve értékét.
     *
     * @param tagName a release tag neve
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    /**
     * Visszaadja a(z) feldolgozási állapot aktuális értékét.
     *
     * @return feldolgozási állapot
     */
    public String getStatus() {
        return status;
    }

    /**
     * Beállítja a(z) feldolgozási állapot értékét.
     *
     * @param status a művelethez átadott {@code status} érték
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Visszaadja a(z) célkönyvtár aktuális értékét.
     *
     * @return célkönyvtár
     */
    public String getTargetDirectory() {
        return targetDirectory;
    }

    /**
     * Beállítja a(z) célkönyvtár értékét.
     *
     * @param targetDirectory a célkönyvtár
     */
    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    /**
     * Visszaadja a(z) telepített artefaktumok aktuális értékét.
     *
     * @return telepített artefaktumok
     */
    public List<InstalledArtifactResult> getInstalledArtifacts() { return installedArtifacts; }

    /**
     * Beállítja a(z) telepített artefaktumok értékét.
     *
     * @param installedArtifacts a művelethez átadott {@code installedArtifacts} érték
     */
    public void setInstalledArtifacts(List<InstalledArtifactResult> installedArtifacts) {
        this.installedArtifacts = installedArtifacts == null ? new ArrayList<>() : new ArrayList<>(installedArtifacts);
    }

    /**
     * Visszaadja a(z) eredményüzenet aktuális értékét.
     *
     * @return eredményüzenet
     */
    public String getMessage() {
        return message;
    }

    /**
     * Beállítja a(z) eredményüzenet értékét.
     *
     * @param message a művelethez átadott {@code message} érték
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
