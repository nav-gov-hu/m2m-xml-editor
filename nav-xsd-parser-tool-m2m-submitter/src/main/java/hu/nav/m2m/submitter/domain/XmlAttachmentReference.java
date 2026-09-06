package hu.nav.m2m.submitter.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * Az XML-ben deklarált csatolmányhivatkozást perzisztáló domain entitás; összeköti a fájlnevet, XML-kontextust és a NAV fileId-t.
 */
@Entity
@Table(name = "m2m_xml_attachment_reference")
public class XmlAttachmentReference {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private M2mSubmission submission;
    @Column(name = "element_name")
    private String elementName;
    @Column(name = "file_id")
    private String fileId;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "sequence_no")
    private Integer sequenceNo;
    @Column(name = "created_at")
    private Instant createdAt;

    /** Inicializálja az új csatolmányhivatkozás technikai azonosítóját és létrehozási időpontját perzisztálás előtt. */
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
     * Visszaadja a(z) elementName aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getElementName() { return elementName; }
    /**
     * Beállítja a(z) elementName értékét a domain objektumon.
     *
     * @param elementName a művelethez átadott {@code elementName} érték
     */
    public void setElementName(String elementName) { this.elementName = elementName; }
    /**
     * Visszaadja a(z) NAV fileId aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getFileId() { return fileId; }
    /**
     * Beállítja a(z) NAV fileId értékét a domain objektumon.
     *
     * @param fileId a művelethez átadott {@code fileId} érték
     */
    public void setFileId(String fileId) { this.fileId = fileId; }
    /**
     * Visszaadja a(z) fájlnév aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getFileName() { return fileName; }
    /**
     * Beállítja a(z) fájlnév értékét a domain objektumon.
     *
     * @param fileName a művelethez átadott {@code fileName} érték
     */
    public void setFileName(String fileName) { this.fileName = fileName; }
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
     * Visszaadja a(z) sequenceNo aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Integer getSequenceNo() { return sequenceNo; }
    /**
     * Beállítja a(z) sequenceNo értékét a domain objektumon.
     *
     * @param sequenceNo a művelethez átadott {@code sequenceNo} érték
     */
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
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
