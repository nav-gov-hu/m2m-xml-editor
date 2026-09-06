package hu.nav.m2m.submitter.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * Egy XML-hez tartozó M2M beküldési munkamenet és életciklus perzisztens gyökérentitása.
 */
@Entity
@Table(name = "m2m_submission")
public class M2mSubmission {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(name = "interface_type")
    private InterfaceType interfaceType;
    @Column(name = "bizonylat_tipus")
    private String bizonylatTipus;
    @Column(name = "bizonylat_verzio")
    private String bizonylatVerzio;
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_mode")
    private GatewayMode gatewayMode = GatewayMode.MOCK;
    @Column(name = "xml_file_id")
    private Long xmlFileId;
    @Column(name = "xml_file_name")
    private String xmlFileName;
    @Column(name = "xml_storage_path")
    private String xmlStoragePath;
    @Column(name = "xml_sha256_hex")
    private String xmlSha256Hex;
    @Column(name = "xml_file_size")
    private Long xmlFileSize;
    @Enumerated(EnumType.STRING)
    private CompressionType compression;
    @Column(name = "nav_file_id")
    private String navFileId;
    @Column(name = "nav_ugy_azonosito")
    private String navUgyAzonosito;
    @Column(name = "nav_erkeztetesi_szam")
    private String navErkeztetesiSzam;
    @Column(name = "nav_status")
    private String navStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "internal_status")
    private SubmissionStatus internalStatus;
    @Column(name = "result_code")
    private String resultCode;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "result_message")
    private String resultMessage;
    @Column(name = "nav_befogadas_idopontja")
    private Instant navBefogadasIdopontja;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_megjegyzes")
    private String navMegjegyzes;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_validacios_hibak")
    private String navValidaciosHibak;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_response_body")
    private String navResponseBody;
    @Column(name = "nav_http_status")
    private Integer navHttpStatus;
    @Column(name = "submission_started_at")
    private Instant submissionStartedAt;
    @Column(name = "submission_finished_at")
    private Instant submissionFinishedAt;
    @Column(name = "submission_duration_ms")
    private Long submissionDurationMs;
    @Column(name = "message_id")
    private String messageId;
    @Column(name = "correlation_id")
    private String correlationId;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "m2m_submit_marked_at")
    private Instant m2mSubmitMarkedAt;
    @Column(name = "m2m_submitted_at")
    private Instant m2mSubmittedAt;
    @Column(name = "m2m_finalized_at")
    private Instant m2mFinalizedAt;
    @Column(name = "m2m_next_poll_at")
    private Instant m2mNextPollAt;
    @Column(name = "m2m_last_poll_at")
    private Instant m2mLastPollAt;
    @Column(name = "m2m_poll_attempts")
    private Integer m2mPollAttempts = 0;
    @Column(name = "m2m_terminal")
    private Boolean m2mTerminal = false;
    @Column(name = "m2m_resubmittable")
    private Boolean m2mResubmittable = true;

    @Column(name = "nav_validacio_ugy_azonosito")
    private String navValidacioUgyAzonosito;
    @Column(name = "nav_validacio_statusz")
    private String navValidacioStatusz;
    @Column(name = "nav_validacio_result_code")
    private String navValidacioResultCode;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_validacio_result_message")
    private String navValidacioResultMessage;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_validacio_hibak")
    private String navValidacioHibak;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_validacios_tanusitvany")
    private String navValidaciosTanusitvany;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_validacio_response_body")
    private String navValidacioResponseBody;
    @Column(name = "nav_validacio_started_at")
    private Instant navValidacioStartedAt;
    @Column(name = "nav_validacio_finished_at")
    private Instant navValidacioFinishedAt;
    @Column(name = "nav_validacio_last_checked_at")
    private Instant navValidacioLastCheckedAt;
    @Column(name = "nav_validacio_message_id")
    private String navValidacioMessageId;
    @Column(name = "nav_validacio_correlation_id")
    private String navValidacioCorrelationId;
    @Column(name = "nav_validacio_payload_sha256")
    private String navValidacioPayloadSha256;
    @Column(name = "fast_track_submission_used")
    private Boolean fastTrackSubmissionUsed = false;

    @Column(name = "nav_kalkulacio_ugy_azonosito")
    private String navKalkulacioUgyAzonosito;
    @Column(name = "nav_kalkulacio_statusz")
    private String navKalkulacioStatusz;
    @Column(name = "nav_kalkulacio_result_code")
    private String navKalkulacioResultCode;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_kalkulacio_result_message")
    private String navKalkulacioResultMessage;
    @Column(name = "nav_kalkulacio_hiba_kod")
    private String navKalkulacioHibaKod;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_kalkulacio_hiba_uzenet")
    private String navKalkulacioHibaUzenet;
    @Column(name = "nav_kalkulacio_mezo_azonosito")
    private String navKalkulacioMezoAzonosito;
    @Column(name = "nav_kalkulacio_szabaly_azonosito")
    private String navKalkulacioSzabalyAzonosito;
    @Column(name = "nav_kalkulacio_tomorites")
    private String navKalkulacioTomorites;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_kalkulalt_xml")
    private String navKalkulaltXml;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nav_kalkulacio_response_body")
    private String navKalkulacioResponseBody;
    @Column(name = "nav_kalkulacio_started_at")
    private Instant navKalkulacioStartedAt;
    @Column(name = "nav_kalkulacio_finished_at")
    private Instant navKalkulacioFinishedAt;
    @Column(name = "nav_kalkulacio_last_checked_at")
    private Instant navKalkulacioLastCheckedAt;
    @Column(name = "nav_kalkulacio_message_id")
    private String navKalkulacioMessageId;
    @Column(name = "nav_kalkulacio_correlation_id")
    private String navKalkulacioCorrelationId;

    /** Inicializálja az új beküldés azonosítóját, létrehozási és módosítási időpontját az első perzisztálás előtt. */
    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (id == null) id = UUID.randomUUID();
    }

    /** Frissíti a beküldés módosítási időpontját minden JPA update művelet előtt. */
    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

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
     * Visszaadja a(z) NAV interfésztípus aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public InterfaceType getInterfaceType() { return interfaceType; }
    /**
     * Beállítja a(z) NAV interfésztípus értékét a domain objektumon.
     *
     * @param interfaceType a művelethez átadott {@code interfaceType} érték
     */
    public void setInterfaceType(InterfaceType interfaceType) { this.interfaceType = interfaceType; }
    /**
     * Visszaadja a(z) bizonylatTipus aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getBizonylatTipus() { return bizonylatTipus; }
    /**
     * Beállítja a(z) bizonylatTipus értékét a domain objektumon.
     *
     * @param bizonylatTipus a művelethez átadott {@code bizonylatTipus} érték
     */
    public void setBizonylatTipus(String bizonylatTipus) { this.bizonylatTipus = bizonylatTipus; }
    /**
     * Visszaadja a(z) bizonylatVerzio aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getBizonylatVerzio() { return bizonylatVerzio; }
    /**
     * Beállítja a(z) bizonylatVerzio értékét a domain objektumon.
     *
     * @param bizonylatVerzio a művelethez átadott {@code bizonylatVerzio} érték
     */
    public void setBizonylatVerzio(String bizonylatVerzio) { this.bizonylatVerzio = bizonylatVerzio; }
    /**
     * Visszaadja a(z) gateway mód aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public GatewayMode getGatewayMode() { return gatewayMode; }
    /**
     * Beállítja a(z) gateway mód értékét a domain objektumon.
     *
     * @param gatewayMode a művelethez átadott {@code gatewayMode} érték
     */
    public void setGatewayMode(GatewayMode gatewayMode) { this.gatewayMode = gatewayMode; }
    /**
     * Visszaadja a(z) XML-fájl azonosító aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Long getXmlFileId() { return xmlFileId; }
    /**
     * Beállítja a(z) XML-fájl azonosító értékét a domain objektumon.
     *
     * @param xmlFileId az érintett XML-fájl adatbázis-azonosítója
     */
    public void setXmlFileId(Long xmlFileId) { this.xmlFileId = xmlFileId; }
    /**
     * Visszaadja a(z) xmlFileName aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getXmlFileName() { return xmlFileName; }
    /**
     * Beállítja a(z) xmlFileName értékét a domain objektumon.
     *
     * @param xmlFileName a művelethez átadott {@code xmlFileName} érték
     */
    public void setXmlFileName(String xmlFileName) { this.xmlFileName = xmlFileName; }
    /**
     * Visszaadja a(z) xmlStoragePath aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getXmlStoragePath() { return xmlStoragePath; }
    /**
     * Beállítja a(z) xmlStoragePath értékét a domain objektumon.
     *
     * @param xmlStoragePath a művelethez átadott {@code xmlStoragePath} érték
     */
    public void setXmlStoragePath(String xmlStoragePath) { this.xmlStoragePath = xmlStoragePath; }
    /**
     * Visszaadja a(z) xmlSha256Hex aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getXmlSha256Hex() { return xmlSha256Hex; }
    /**
     * Beállítja a(z) xmlSha256Hex értékét a domain objektumon.
     *
     * @param xmlSha256Hex a művelethez átadott {@code xmlSha256Hex} érték
     */
    public void setXmlSha256Hex(String xmlSha256Hex) { this.xmlSha256Hex = xmlSha256Hex; }
    /**
     * Visszaadja a(z) xmlFileSize aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Long getXmlFileSize() { return xmlFileSize; }
    /**
     * Beállítja a(z) xmlFileSize értékét a domain objektumon.
     *
     * @param xmlFileSize a művelethez átadott {@code xmlFileSize} érték
     */
    public void setXmlFileSize(Long xmlFileSize) { this.xmlFileSize = xmlFileSize; }
    /**
     * Visszaadja a(z) compression aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public CompressionType getCompression() { return compression; }
    /**
     * Beállítja a(z) compression értékét a domain objektumon.
     *
     * @param compression a művelethez átadott {@code compression} érték
     */
    public void setCompression(CompressionType compression) { this.compression = compression; }
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
     * Visszaadja a(z) navUgyAzonosito aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavUgyAzonosito() { return navUgyAzonosito; }
    /**
     * Beállítja a(z) navUgyAzonosito értékét a domain objektumon.
     *
     * @param navUgyAzonosito a művelethez átadott {@code navUgyAzonosito} érték
     */
    public void setNavUgyAzonosito(String navUgyAzonosito) { this.navUgyAzonosito = navUgyAzonosito; }
    /**
     * Visszaadja a(z) navErkeztetesiSzam aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavErkeztetesiSzam() { return navErkeztetesiSzam; }
    /**
     * Beállítja a(z) navErkeztetesiSzam értékét a domain objektumon.
     *
     * @param navErkeztetesiSzam a művelethez átadott {@code navErkeztetesiSzam} érték
     */
    public void setNavErkeztetesiSzam(String navErkeztetesiSzam) { this.navErkeztetesiSzam = navErkeztetesiSzam; }
    /**
     * Visszaadja a(z) navStatus aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavStatus() { return navStatus; }
    /**
     * Beállítja a(z) navStatus értékét a domain objektumon.
     *
     * @param navStatus a NAV válaszában kapott státuszérték
     */
    public void setNavStatus(String navStatus) { this.navStatus = navStatus; }
    /**
     * Visszaadja a(z) internalStatus aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public SubmissionStatus getInternalStatus() { return internalStatus; }
    /**
     * Beállítja a(z) internalStatus értékét a domain objektumon.
     *
     * @param internalStatus a művelethez átadott {@code internalStatus} érték
     */
    public void setInternalStatus(SubmissionStatus internalStatus) { this.internalStatus = internalStatus; }
    /**
     * Visszaadja a(z) NAV eredménykód aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getResultCode() { return resultCode; }
    /**
     * Beállítja a(z) NAV eredménykód értékét a domain objektumon.
     *
     * @param resultCode a NAV eredménykód
     */
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    /**
     * Visszaadja a(z) resultMessage aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getResultMessage() { return resultMessage; }
    /**
     * Beállítja a(z) resultMessage értékét a domain objektumon.
     *
     * @param resultMessage a művelethez átadott {@code resultMessage} érték
     */
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    /**
     * Visszaadja a(z) navBefogadasIdopontja aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavBefogadasIdopontja() { return navBefogadasIdopontja; }
    /**
     * Beállítja a(z) navBefogadasIdopontja értékét a domain objektumon.
     *
     * @param navBefogadasIdopontja a művelethez átadott {@code navBefogadasIdopontja} érték
     */
    public void setNavBefogadasIdopontja(Instant navBefogadasIdopontja) { this.navBefogadasIdopontja = navBefogadasIdopontja; }
    /**
     * Visszaadja a(z) navMegjegyzes aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavMegjegyzes() { return navMegjegyzes; }
    /**
     * Beállítja a(z) navMegjegyzes értékét a domain objektumon.
     *
     * @param navMegjegyzes a művelethez átadott {@code navMegjegyzes} érték
     */
    public void setNavMegjegyzes(String navMegjegyzes) { this.navMegjegyzes = navMegjegyzes; }
    /**
     * Visszaadja a(z) navValidaciosHibak aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidaciosHibak() { return navValidaciosHibak; }
    /**
     * Beállítja a(z) navValidaciosHibak értékét a domain objektumon.
     *
     * @param navValidaciosHibak a művelethez átadott {@code navValidaciosHibak} érték
     */
    public void setNavValidaciosHibak(String navValidaciosHibak) { this.navValidaciosHibak = navValidaciosHibak; }
    /**
     * Visszaadja a(z) navResponseBody aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavResponseBody() { return navResponseBody; }
    /**
     * Beállítja a(z) navResponseBody értékét a domain objektumon.
     *
     * @param navResponseBody a művelethez átadott {@code navResponseBody} érték
     */
    public void setNavResponseBody(String navResponseBody) { this.navResponseBody = navResponseBody; }
    /**
     * Visszaadja a(z) navHttpStatus aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Integer getNavHttpStatus() { return navHttpStatus; }
    /**
     * Beállítja a(z) navHttpStatus értékét a domain objektumon.
     *
     * @param navHttpStatus a művelethez átadott {@code navHttpStatus} érték
     */
    public void setNavHttpStatus(Integer navHttpStatus) { this.navHttpStatus = navHttpStatus; }
    /**
     * Visszaadja a(z) submissionStartedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getSubmissionStartedAt() { return submissionStartedAt; }
    /**
     * Beállítja a(z) submissionStartedAt értékét a domain objektumon.
     *
     * @param submissionStartedAt a művelethez átadott {@code submissionStartedAt} érték
     */
    public void setSubmissionStartedAt(Instant submissionStartedAt) { this.submissionStartedAt = submissionStartedAt; }
    /**
     * Visszaadja a(z) submissionFinishedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getSubmissionFinishedAt() { return submissionFinishedAt; }
    /**
     * Beállítja a(z) submissionFinishedAt értékét a domain objektumon.
     *
     * @param submissionFinishedAt a művelethez átadott {@code submissionFinishedAt} érték
     */
    public void setSubmissionFinishedAt(Instant submissionFinishedAt) { this.submissionFinishedAt = submissionFinishedAt; }
    /**
     * Visszaadja a(z) submissionDurationMs aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Long getSubmissionDurationMs() { return submissionDurationMs; }
    /**
     * Beállítja a(z) submissionDurationMs értékét a domain objektumon.
     *
     * @param submissionDurationMs a művelethez átadott {@code submissionDurationMs} érték
     */
    public void setSubmissionDurationMs(Long submissionDurationMs) { this.submissionDurationMs = submissionDurationMs; }
    /**
     * Visszaadja a(z) NAV messageId aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getMessageId() { return messageId; }
    /**
     * Beállítja a(z) NAV messageId értékét a domain objektumon.
     *
     * @param messageId a NAV kérés egyedi messageId értéke
     */
    public void setMessageId(String messageId) { this.messageId = messageId; }
    /**
     * Visszaadja a(z) korrelációs azonosító aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getCorrelationId() { return correlationId; }
    /**
     * Beállítja a(z) korrelációs azonosító értékét a domain objektumon.
     *
     * @param correlationId a művelethez átadott {@code correlationId} érték
     */
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
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
    /**
     * Visszaadja a(z) utolsó módosítás időpontja aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getUpdatedAt() { return updatedAt; }
    /**
     * Beállítja a(z) utolsó módosítás időpontja értékét a domain objektumon.
     *
     * @param updatedAt a művelethez átadott {@code updatedAt} érték
     */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Visszaadja a(z) m2mSubmitMarkedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getM2mSubmitMarkedAt() { return m2mSubmitMarkedAt; }
    /**
     * Beállítja a(z) m2mSubmitMarkedAt értékét a domain objektumon.
     *
     * @param m2mSubmitMarkedAt a művelethez átadott {@code m2mSubmitMarkedAt} érték
     */
    public void setM2mSubmitMarkedAt(Instant m2mSubmitMarkedAt) { this.m2mSubmitMarkedAt = m2mSubmitMarkedAt; }
    /**
     * Visszaadja a(z) m2mSubmittedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getM2mSubmittedAt() { return m2mSubmittedAt; }
    /**
     * Beállítja a(z) m2mSubmittedAt értékét a domain objektumon.
     *
     * @param m2mSubmittedAt a művelethez átadott {@code m2mSubmittedAt} érték
     */
    public void setM2mSubmittedAt(Instant m2mSubmittedAt) { this.m2mSubmittedAt = m2mSubmittedAt; }
    /**
     * Visszaadja a(z) m2mFinalizedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getM2mFinalizedAt() { return m2mFinalizedAt; }
    /**
     * Beállítja a(z) m2mFinalizedAt értékét a domain objektumon.
     *
     * @param m2mFinalizedAt a művelethez átadott {@code m2mFinalizedAt} érték
     */
    public void setM2mFinalizedAt(Instant m2mFinalizedAt) { this.m2mFinalizedAt = m2mFinalizedAt; }
    /**
     * Visszaadja a(z) m2mNextPollAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getM2mNextPollAt() { return m2mNextPollAt; }
    /**
     * Beállítja a(z) m2mNextPollAt értékét a domain objektumon.
     *
     * @param m2mNextPollAt a művelethez átadott {@code m2mNextPollAt} érték
     */
    public void setM2mNextPollAt(Instant m2mNextPollAt) { this.m2mNextPollAt = m2mNextPollAt; }
    /**
     * Visszaadja a(z) m2mLastPollAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getM2mLastPollAt() { return m2mLastPollAt; }
    /**
     * Beállítja a(z) m2mLastPollAt értékét a domain objektumon.
     *
     * @param m2mLastPollAt a művelethez átadott {@code m2mLastPollAt} érték
     */
    public void setM2mLastPollAt(Instant m2mLastPollAt) { this.m2mLastPollAt = m2mLastPollAt; }
    /**
     * Visszaadja a(z) m2mPollAttempts aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Integer getM2mPollAttempts() { return m2mPollAttempts; }
    /**
     * Beállítja a(z) m2mPollAttempts értékét a domain objektumon.
     *
     * @param m2mPollAttempts a művelethez átadott {@code m2mPollAttempts} érték
     */
    public void setM2mPollAttempts(Integer m2mPollAttempts) { this.m2mPollAttempts = m2mPollAttempts; }
    /**
     * Visszaadja a(z) m2mTerminal aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Boolean getM2mTerminal() { return m2mTerminal; }
    /**
     * Beállítja a(z) m2mTerminal értékét a domain objektumon.
     *
     * @param m2mTerminal a művelethez átadott {@code m2mTerminal} érték
     */
    public void setM2mTerminal(Boolean m2mTerminal) { this.m2mTerminal = m2mTerminal; }
    /**
     * Visszaadja a(z) m2mResubmittable aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Boolean getM2mResubmittable() { return m2mResubmittable; }
    /**
     * Beállítja a(z) m2mResubmittable értékét a domain objektumon.
     *
     * @param m2mResubmittable a művelethez átadott {@code m2mResubmittable} érték
     */
    public void setM2mResubmittable(Boolean m2mResubmittable) { this.m2mResubmittable = m2mResubmittable; }

    /**
     * Visszaadja a(z) navValidacioUgyAzonosito aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidacioUgyAzonosito() { return navValidacioUgyAzonosito; }
    /**
     * Beállítja a(z) navValidacioUgyAzonosito értékét a domain objektumon.
     *
     * @param navValidacioUgyAzonosito a művelethez átadott {@code navValidacioUgyAzonosito} érték
     */
    public void setNavValidacioUgyAzonosito(String navValidacioUgyAzonosito) { this.navValidacioUgyAzonosito = navValidacioUgyAzonosito; }
    /**
     * Visszaadja a(z) navValidacioStatusz aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidacioStatusz() { return navValidacioStatusz; }
    /**
     * Beállítja a(z) navValidacioStatusz értékét a domain objektumon.
     *
     * @param navValidacioStatusz a művelethez átadott {@code navValidacioStatusz} érték
     */
    public void setNavValidacioStatusz(String navValidacioStatusz) { this.navValidacioStatusz = navValidacioStatusz; }
    /**
     * Visszaadja a(z) navValidacioResultCode aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidacioResultCode() { return navValidacioResultCode; }
    /**
     * Beállítja a(z) navValidacioResultCode értékét a domain objektumon.
     *
     * @param navValidacioResultCode a művelethez átadott {@code navValidacioResultCode} érték
     */
    public void setNavValidacioResultCode(String navValidacioResultCode) { this.navValidacioResultCode = navValidacioResultCode; }
    /**
     * Visszaadja a(z) navValidacioResultMessage aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidacioResultMessage() { return navValidacioResultMessage; }
    /**
     * Beállítja a(z) navValidacioResultMessage értékét a domain objektumon.
     *
     * @param navValidacioResultMessage a művelethez átadott {@code navValidacioResultMessage} érték
     */
    public void setNavValidacioResultMessage(String navValidacioResultMessage) { this.navValidacioResultMessage = navValidacioResultMessage; }
    /**
     * Visszaadja a(z) navValidacioHibak aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidacioHibak() { return navValidacioHibak; }
    /**
     * Beállítja a(z) navValidacioHibak értékét a domain objektumon.
     *
     * @param navValidacioHibak a művelethez átadott {@code navValidacioHibak} érték
     */
    public void setNavValidacioHibak(String navValidacioHibak) { this.navValidacioHibak = navValidacioHibak; }
    /**
     * Visszaadja a(z) navValidaciosTanusitvany aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidaciosTanusitvany() { return navValidaciosTanusitvany; }
    /**
     * Beállítja a(z) navValidaciosTanusitvany értékét a domain objektumon.
     *
     * @param navValidaciosTanusitvany a művelethez átadott {@code navValidaciosTanusitvany} érték
     */
    public void setNavValidaciosTanusitvany(String navValidaciosTanusitvany) { this.navValidaciosTanusitvany = navValidaciosTanusitvany; }
    /**
     * Visszaadja a(z) navValidacioResponseBody aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidacioResponseBody() { return navValidacioResponseBody; }
    /**
     * Beállítja a(z) navValidacioResponseBody értékét a domain objektumon.
     *
     * @param navValidacioResponseBody a művelethez átadott {@code navValidacioResponseBody} érték
     */
    public void setNavValidacioResponseBody(String navValidacioResponseBody) { this.navValidacioResponseBody = navValidacioResponseBody; }
    /**
     * Visszaadja a(z) navValidacioStartedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavValidacioStartedAt() { return navValidacioStartedAt; }
    /**
     * Beállítja a(z) navValidacioStartedAt értékét a domain objektumon.
     *
     * @param navValidacioStartedAt a művelethez átadott {@code navValidacioStartedAt} érték
     */
    public void setNavValidacioStartedAt(Instant navValidacioStartedAt) { this.navValidacioStartedAt = navValidacioStartedAt; }
    /**
     * Visszaadja a(z) navValidacioFinishedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavValidacioFinishedAt() { return navValidacioFinishedAt; }
    /**
     * Beállítja a(z) navValidacioFinishedAt értékét a domain objektumon.
     *
     * @param navValidacioFinishedAt a művelethez átadott {@code navValidacioFinishedAt} érték
     */
    public void setNavValidacioFinishedAt(Instant navValidacioFinishedAt) { this.navValidacioFinishedAt = navValidacioFinishedAt; }
    /**
     * Visszaadja a(z) navValidacioLastCheckedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavValidacioLastCheckedAt() { return navValidacioLastCheckedAt; }
    /**
     * Beállítja a(z) navValidacioLastCheckedAt értékét a domain objektumon.
     *
     * @param navValidacioLastCheckedAt a művelethez átadott {@code navValidacioLastCheckedAt} érték
     */
    public void setNavValidacioLastCheckedAt(Instant navValidacioLastCheckedAt) { this.navValidacioLastCheckedAt = navValidacioLastCheckedAt; }
    /**
     * Visszaadja a(z) navValidacioMessageId aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidacioMessageId() { return navValidacioMessageId; }
    /**
     * Beállítja a(z) navValidacioMessageId értékét a domain objektumon.
     *
     * @param navValidacioMessageId a művelethez átadott {@code navValidacioMessageId} érték
     */
    public void setNavValidacioMessageId(String navValidacioMessageId) { this.navValidacioMessageId = navValidacioMessageId; }
    /**
     * Visszaadja a(z) navValidacioCorrelationId aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidacioCorrelationId() { return navValidacioCorrelationId; }
    /**
     * Beállítja a(z) navValidacioCorrelationId értékét a domain objektumon.
     *
     * @param navValidacioCorrelationId a művelethez átadott {@code navValidacioCorrelationId} érték
     */
    public void setNavValidacioCorrelationId(String navValidacioCorrelationId) { this.navValidacioCorrelationId = navValidacioCorrelationId; }
    /**
     * Visszaadja a(z) navValidacioPayloadSha256 aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavValidacioPayloadSha256() { return navValidacioPayloadSha256; }
    /**
     * Beállítja a(z) navValidacioPayloadSha256 értékét a domain objektumon.
     *
     * @param navValidacioPayloadSha256 a művelethez átadott {@code navValidacioPayloadSha256} érték
     */
    public void setNavValidacioPayloadSha256(String navValidacioPayloadSha256) { this.navValidacioPayloadSha256 = navValidacioPayloadSha256; }
    /**
     * Visszaadja a(z) fastTrackSubmissionUsed aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Boolean getFastTrackSubmissionUsed() { return fastTrackSubmissionUsed; }
    /**
     * Beállítja a(z) fastTrackSubmissionUsed értékét a domain objektumon.
     *
     * @param fastTrackSubmissionUsed a művelethez átadott {@code fastTrackSubmissionUsed} érték
     */
    public void setFastTrackSubmissionUsed(Boolean fastTrackSubmissionUsed) { this.fastTrackSubmissionUsed = fastTrackSubmissionUsed; }

    /**
     * Visszaadja a(z) navKalkulacioUgyAzonosito aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioUgyAzonosito() { return navKalkulacioUgyAzonosito; }
    /**
     * Beállítja a(z) navKalkulacioUgyAzonosito értékét a domain objektumon.
     *
     * @param navKalkulacioUgyAzonosito a művelethez átadott {@code navKalkulacioUgyAzonosito} érték
     */
    public void setNavKalkulacioUgyAzonosito(String navKalkulacioUgyAzonosito) { this.navKalkulacioUgyAzonosito = navKalkulacioUgyAzonosito; }
    /**
     * Visszaadja a(z) navKalkulacioStatusz aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioStatusz() { return navKalkulacioStatusz; }
    /**
     * Beállítja a(z) navKalkulacioStatusz értékét a domain objektumon.
     *
     * @param navKalkulacioStatusz a művelethez átadott {@code navKalkulacioStatusz} érték
     */
    public void setNavKalkulacioStatusz(String navKalkulacioStatusz) { this.navKalkulacioStatusz = navKalkulacioStatusz; }
    /**
     * Visszaadja a(z) navKalkulacioResultCode aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioResultCode() { return navKalkulacioResultCode; }
    /**
     * Beállítja a(z) navKalkulacioResultCode értékét a domain objektumon.
     *
     * @param navKalkulacioResultCode a művelethez átadott {@code navKalkulacioResultCode} érték
     */
    public void setNavKalkulacioResultCode(String navKalkulacioResultCode) { this.navKalkulacioResultCode = navKalkulacioResultCode; }
    /**
     * Visszaadja a(z) navKalkulacioResultMessage aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioResultMessage() { return navKalkulacioResultMessage; }
    /**
     * Beállítja a(z) navKalkulacioResultMessage értékét a domain objektumon.
     *
     * @param navKalkulacioResultMessage a művelethez átadott {@code navKalkulacioResultMessage} érték
     */
    public void setNavKalkulacioResultMessage(String navKalkulacioResultMessage) { this.navKalkulacioResultMessage = navKalkulacioResultMessage; }
    /**
     * Visszaadja a(z) navKalkulacioHibaKod aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioHibaKod() { return navKalkulacioHibaKod; }
    /**
     * Beállítja a(z) navKalkulacioHibaKod értékét a domain objektumon.
     *
     * @param navKalkulacioHibaKod a művelethez átadott {@code navKalkulacioHibaKod} érték
     */
    public void setNavKalkulacioHibaKod(String navKalkulacioHibaKod) { this.navKalkulacioHibaKod = navKalkulacioHibaKod; }
    /**
     * Visszaadja a(z) navKalkulacioHibaUzenet aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioHibaUzenet() { return navKalkulacioHibaUzenet; }
    /**
     * Beállítja a(z) navKalkulacioHibaUzenet értékét a domain objektumon.
     *
     * @param navKalkulacioHibaUzenet a művelethez átadott {@code navKalkulacioHibaUzenet} érték
     */
    public void setNavKalkulacioHibaUzenet(String navKalkulacioHibaUzenet) { this.navKalkulacioHibaUzenet = navKalkulacioHibaUzenet; }
    /**
     * Visszaadja a(z) navKalkulacioMezoAzonosito aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioMezoAzonosito() { return navKalkulacioMezoAzonosito; }
    /**
     * Beállítja a(z) navKalkulacioMezoAzonosito értékét a domain objektumon.
     *
     * @param navKalkulacioMezoAzonosito a művelethez átadott {@code navKalkulacioMezoAzonosito} érték
     */
    public void setNavKalkulacioMezoAzonosito(String navKalkulacioMezoAzonosito) { this.navKalkulacioMezoAzonosito = navKalkulacioMezoAzonosito; }
    /**
     * Visszaadja a(z) navKalkulacioSzabalyAzonosito aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioSzabalyAzonosito() { return navKalkulacioSzabalyAzonosito; }
    /**
     * Beállítja a(z) navKalkulacioSzabalyAzonosito értékét a domain objektumon.
     *
     * @param navKalkulacioSzabalyAzonosito a művelethez átadott {@code navKalkulacioSzabalyAzonosito} érték
     */
    public void setNavKalkulacioSzabalyAzonosito(String navKalkulacioSzabalyAzonosito) { this.navKalkulacioSzabalyAzonosito = navKalkulacioSzabalyAzonosito; }
    /**
     * Visszaadja a(z) navKalkulacioTomorites aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioTomorites() { return navKalkulacioTomorites; }
    /**
     * Beállítja a(z) navKalkulacioTomorites értékét a domain objektumon.
     *
     * @param navKalkulacioTomorites a művelethez átadott {@code navKalkulacioTomorites} érték
     */
    public void setNavKalkulacioTomorites(String navKalkulacioTomorites) { this.navKalkulacioTomorites = navKalkulacioTomorites; }
    /**
     * Visszaadja a(z) navKalkulaltXml aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulaltXml() { return navKalkulaltXml; }
    /**
     * Beállítja a(z) navKalkulaltXml értékét a domain objektumon.
     *
     * @param navKalkulaltXml a művelethez átadott {@code navKalkulaltXml} érték
     */
    public void setNavKalkulaltXml(String navKalkulaltXml) { this.navKalkulaltXml = navKalkulaltXml; }
    /**
     * Visszaadja a(z) navKalkulacioResponseBody aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioResponseBody() { return navKalkulacioResponseBody; }
    /**
     * Beállítja a(z) navKalkulacioResponseBody értékét a domain objektumon.
     *
     * @param navKalkulacioResponseBody a művelethez átadott {@code navKalkulacioResponseBody} érték
     */
    public void setNavKalkulacioResponseBody(String navKalkulacioResponseBody) { this.navKalkulacioResponseBody = navKalkulacioResponseBody; }
    /**
     * Visszaadja a(z) navKalkulacioStartedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavKalkulacioStartedAt() { return navKalkulacioStartedAt; }
    /**
     * Beállítja a(z) navKalkulacioStartedAt értékét a domain objektumon.
     *
     * @param navKalkulacioStartedAt a művelethez átadott {@code navKalkulacioStartedAt} érték
     */
    public void setNavKalkulacioStartedAt(Instant navKalkulacioStartedAt) { this.navKalkulacioStartedAt = navKalkulacioStartedAt; }
    /**
     * Visszaadja a(z) navKalkulacioFinishedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavKalkulacioFinishedAt() { return navKalkulacioFinishedAt; }
    /**
     * Beállítja a(z) navKalkulacioFinishedAt értékét a domain objektumon.
     *
     * @param navKalkulacioFinishedAt a művelethez átadott {@code navKalkulacioFinishedAt} érték
     */
    public void setNavKalkulacioFinishedAt(Instant navKalkulacioFinishedAt) { this.navKalkulacioFinishedAt = navKalkulacioFinishedAt; }
    /**
     * Visszaadja a(z) navKalkulacioLastCheckedAt aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getNavKalkulacioLastCheckedAt() { return navKalkulacioLastCheckedAt; }
    /**
     * Beállítja a(z) navKalkulacioLastCheckedAt értékét a domain objektumon.
     *
     * @param navKalkulacioLastCheckedAt a művelethez átadott {@code navKalkulacioLastCheckedAt} érték
     */
    public void setNavKalkulacioLastCheckedAt(Instant navKalkulacioLastCheckedAt) { this.navKalkulacioLastCheckedAt = navKalkulacioLastCheckedAt; }
    /**
     * Visszaadja a(z) navKalkulacioMessageId aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioMessageId() { return navKalkulacioMessageId; }
    /**
     * Beállítja a(z) navKalkulacioMessageId értékét a domain objektumon.
     *
     * @param navKalkulacioMessageId a művelethez átadott {@code navKalkulacioMessageId} érték
     */
    public void setNavKalkulacioMessageId(String navKalkulacioMessageId) { this.navKalkulacioMessageId = navKalkulacioMessageId; }
    /**
     * Visszaadja a(z) navKalkulacioCorrelationId aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavKalkulacioCorrelationId() { return navKalkulacioCorrelationId; }
    /**
     * Beállítja a(z) navKalkulacioCorrelationId értékét a domain objektumon.
     *
     * @param navKalkulacioCorrelationId a művelethez átadott {@code navKalkulacioCorrelationId} érték
     */
    public void setNavKalkulacioCorrelationId(String navKalkulacioCorrelationId) { this.navKalkulacioCorrelationId = navKalkulacioCorrelationId; }
}

