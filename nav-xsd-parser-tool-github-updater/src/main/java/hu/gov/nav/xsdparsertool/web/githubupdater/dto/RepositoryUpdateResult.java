package hu.gov.nav.xsdparsertool.web.githubupdater.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Egy GitHub repository frissítésének összesített eredményét és tag-szintű részleteit reprezentáló DTO.
 */
public class RepositoryUpdateResult {
    private String repositoryName;
    private String localHighestTag;
    private int remoteTagCount;
    private int downloadedCount;
    private int skippedCount;
    private int failedCount;
    private List<TagUpdateResult> tags = new ArrayList<>();

    /**
     * Visszaadja a(z) repository neve aktuális értékét.
     *
     * @return repository neve
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Beállítja a(z) repository neve értékét.
     *
     * @param repositoryName a GitHub repository neve
     */
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    /**
     * Visszaadja a(z) lokálisan legmagasabb tag aktuális értékét.
     *
     * @return lokálisan legmagasabb tag
     */
    public String getLocalHighestTag() {
        return localHighestTag;
    }

    /**
     * Beállítja a(z) lokálisan legmagasabb tag értékét.
     *
     * @param localHighestTag a művelethez átadott {@code localHighestTag} érték
     */
    public void setLocalHighestTag(String localHighestTag) {
        this.localHighestTag = localHighestTag;
    }

    /**
     * Visszaadja a(z) távoli tagek száma aktuális értékét.
     *
     * @return távoli tagek száma
     */
    public int getRemoteTagCount() {
        return remoteTagCount;
    }

    /**
     * Beállítja a(z) távoli tagek száma értékét.
     *
     * @param remoteTagCount a művelethez átadott {@code remoteTagCount} érték
     */
    public void setRemoteTagCount(int remoteTagCount) {
        this.remoteTagCount = remoteTagCount;
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
     * Visszaadja a(z) tag-szintű eredmények aktuális értékét.
     *
     * @return tag-szintű eredmények
     */
    public List<TagUpdateResult> getTags() {
        return tags;
    }

    /**
     * Beállítja a(z) tag-szintű eredmények értékét.
     *
     * @param tags a művelethez átadott {@code tags} érték
     */
    public void setTags(List<TagUpdateResult> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }
}
