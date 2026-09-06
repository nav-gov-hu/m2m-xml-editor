package hu.nav.m2m.submitter.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * Egy M2M beküldéshez tartozó csatolmány perzisztens domain entitása, a lokális tárolási adatokkal és NAV fileId-val.
 */
@Entity
@Table(name = "m2m_attachment")
public class M2mAttachment {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private M2mSubmission submission;
    @Column(name = "original_file_name")
    private String originalFileName;
    @Column(name = "storage_path")
    private String storagePath;
    @Column(name = "sha256_hex")
    private String sha256Hex;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "nav_file_id")
    private String navFileId;
    @Column(name = "nav_uploaded_at")
    private Instant navUploadedAt;
    @Column(name = "nav_expires_at")
    private Instant navExpiresAt;
    @Column(name = "nav_last_refreshed_at")
    private Instant navLastRefreshedAt;
    @Column(name = "nav_upload_result_code")
    private String navUploadResultCode;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_upload_result_message")
    private String navUploadResultMessage;
    @Column(name = "xml_reference_present")
    private boolean xmlReferencePresent;
    @Column(name = "created_at")
    private Instant createdAt;

    /** Inicializálja az új M2M csatolmány technikai azonosítóját és létrehozási időpontját perzisztálás előtt. */
    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
    /**
     * Visszaadja a(z) azonosító aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public UUID getId() { return id; }
    /**
     * Beállítja a(z) azonosító értékét a domain objektumon.
     *
     * @param id a művelethez átadott {@code id} érték
     */
    public void setId(UUID id) { this.id = id; }
    /**
     * Visszaadja a(z) submission aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public M2mSubmission getSubmission() { return submission; }
    /**
     * Beállítja a(z) submission értékét a domain objektumon.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    public void setSubmission(M2mSubmission submission) { this.submission = submission; }
    /**
     * Visszaadja a(z) originalFileName aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getOriginalFileName() { return originalFileName; }
    /**
     * Beállítja a(z) originalFileName értékét a domain objektumon.
     *
     * @param originalFileName a művelethez átadott {@code originalFileName} érték
     */
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    /**
     * Visszaadja a(z) tárolási útvonal aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getStoragePath() { return storagePath; }
    /**
     * Beállítja a(z) tárolási útvonal értékét a domain objektumon.
     *
     * @param storagePath a művelethez átadott {@code storagePath} érték
     */
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    /**
     * Visszaadja a(z) SHA-256 ellenőrzőösszeg aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getSha256Hex() { return sha256Hex; }
    /**
     * Beállítja a(z) SHA-256 ellenőrzőösszeg értékét a domain objektumon.
     *
     * @param sha256Hex a művelethez átadott {@code sha256Hex} érték
     */
    public void setSha256Hex(String sha256Hex) { this.sha256Hex = sha256Hex; }
    /**
     * Visszaadja a(z) fájlméret aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Long getFileSize() { return fileSize; }
    /**
     * Beállítja a(z) fájlméret értékét a domain objektumon.
     *
     * @param fileSize a művelethez átadott {@code fileSize} érték
     */
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    /**
     * Visszaadja a(z) navFileId aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavFileId() { return navFileId; }
    /**
     * Beállítja a(z) navFileId értékét a domain objektumon.
     *
     * @param navFileId a művelethez átadott {@code navFileId} érték
     */
    public void setNavFileId(String navFileId) { this.navFileId = navFileId; }
    /**
     * Visszaadja a(z) navUploadedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavUploadedAt() { return navUploadedAt; }
    /**
     * Beállítja a(z) navUploadedAt értékét a domain objektumon.
     *
     * @param navUploadedAt a művelethez átadott {@code navUploadedAt} érték
     */
    public void setNavUploadedAt(Instant navUploadedAt) { this.navUploadedAt = navUploadedAt; }
    /**
     * Visszaadja a(z) navExpiresAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavExpiresAt() { return navExpiresAt; }
    /**
     * Beállítja a(z) navExpiresAt értékét a domain objektumon.
     *
     * @param navExpiresAt a művelethez átadott {@code navExpiresAt} érték
     */
    public void setNavExpiresAt(Instant navExpiresAt) { this.navExpiresAt = navExpiresAt; }
    /**
     * Visszaadja a(z) navLastRefreshedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavLastRefreshedAt() { return navLastRefreshedAt; }
    /**
     * Beállítja a(z) navLastRefreshedAt értékét a domain objektumon.
     *
     * @param navLastRefreshedAt a művelethez átadott {@code navLastRefreshedAt} érték
     */
    public void setNavLastRefreshedAt(Instant navLastRefreshedAt) { this.navLastRefreshedAt = navLastRefreshedAt; }
    /**
     * Visszaadja a(z) navUploadResultCode aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavUploadResultCode() { return navUploadResultCode; }
    /**
     * Beállítja a(z) navUploadResultCode értékét a domain objektumon.
     *
     * @param navUploadResultCode a művelethez átadott {@code navUploadResultCode} érték
     */
    public void setNavUploadResultCode(String navUploadResultCode) { this.navUploadResultCode = navUploadResultCode; }
    /**
     * Visszaadja a(z) navUploadResultMessage aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavUploadResultMessage() { return navUploadResultMessage; }
    /**
     * Beállítja a(z) navUploadResultMessage értékét a domain objektumon.
     *
     * @param navUploadResultMessage a művelethez átadott {@code navUploadResultMessage} érték
     */
    public void setNavUploadResultMessage(String navUploadResultMessage) { this.navUploadResultMessage = navUploadResultMessage; }
    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isXmlReferencePresent() { return xmlReferencePresent; }
    /**
     * Beállítja a(z) xmlReferencePresent értékét a domain objektumon.
     *
     * @param xmlReferencePresent a művelethez átadott {@code xmlReferencePresent} érték
     */
    public void setXmlReferencePresent(boolean xmlReferencePresent) { this.xmlReferencePresent = xmlReferencePresent; }
    /**
     * Visszaadja a(z) létrehozási időpont aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getCreatedAt() { return createdAt; }
    /**
     * Beállítja a(z) létrehozási időpont értékét a domain objektumon.
     *
     * @param createdAt a művelethez átadott {@code createdAt} érték
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
