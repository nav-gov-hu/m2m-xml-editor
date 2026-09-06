package hu.gov.nav.xsdparsertool.web.githubupdater.dto;

/**
 * Egy release-ből felismert és típus szerinti célkönyvtárba telepített artefaktum eredményét reprezentáló DTO.
 */
public class InstalledArtifactResult {
    private String artifactType;
    private String sourceFile;
    private String targetFile;
    private String status;
    private String message;

    /**
     * Létrehozza a(z) {@code InstalledArtifactResult} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     */
    public InstalledArtifactResult() {}

    /**
     * Létrehozza a(z) {@code InstalledArtifactResult} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param artifactType a művelethez átadott {@code artifactType} érték
     * @param sourceFile a művelethez átadott {@code sourceFile} érték
     * @param targetFile a művelethez átadott {@code targetFile} érték
     * @param status a művelethez átadott {@code status} érték
     * @param message a művelethez átadott {@code message} érték
     */
    public InstalledArtifactResult(String artifactType, String sourceFile, String targetFile, String status, String message) {
        this.artifactType = artifactType;
        this.sourceFile = sourceFile;
        this.targetFile = targetFile;
        this.status = status;
        this.message = message;
    }

    /**
     * Visszaadja a(z) artefaktum típusa aktuális értékét.
     *
     * @return artefaktum típusa
     */
    public String getArtifactType() { return artifactType; }
    /**
     * Beállítja a(z) artefaktum típusa értékét.
     *
     * @param artifactType a művelethez átadott {@code artifactType} érték
     */
    public void setArtifactType(String artifactType) { this.artifactType = artifactType; }
    /**
     * Visszaadja a(z) forrásfájl útvonala aktuális értékét.
     *
     * @return forrásfájl útvonala
     */
    public String getSourceFile() { return sourceFile; }
    /**
     * Beállítja a(z) forrásfájl útvonala értékét.
     *
     * @param sourceFile a művelethez átadott {@code sourceFile} érték
     */
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    /**
     * Visszaadja a(z) célfájl útvonala aktuális értékét.
     *
     * @return célfájl útvonala
     */
    public String getTargetFile() { return targetFile; }
    /**
     * Beállítja a(z) célfájl útvonala értékét.
     *
     * @param targetFile a művelethez átadott {@code targetFile} érték
     */
    public void setTargetFile(String targetFile) { this.targetFile = targetFile; }
    /**
     * Visszaadja a(z) feldolgozási állapot aktuális értékét.
     *
     * @return feldolgozási állapot
     */
    public String getStatus() { return status; }
    /**
     * Beállítja a(z) feldolgozási állapot értékét.
     *
     * @param status a művelethez átadott {@code status} érték
     */
    public void setStatus(String status) { this.status = status; }
    /**
     * Visszaadja a(z) eredményüzenet aktuális értékét.
     *
     * @return eredményüzenet
     */
    public String getMessage() { return message; }
    /**
     * Beállítja a(z) eredményüzenet értékét.
     *
     * @param message a művelethez átadott {@code message} érték
     */
    public void setMessage(String message) { this.message = message; }
}
