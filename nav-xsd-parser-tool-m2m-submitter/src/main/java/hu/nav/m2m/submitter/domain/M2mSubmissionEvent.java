package hu.nav.m2m.submitter.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

/**
 * Egy M2M beküldéshez tartozó kommunikációs vagy életciklus-esemény perzisztens naplóbejegyzése.
 */
@Entity
@Table(name = "m2m_submission_event")
public class M2mSubmissionEvent {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private M2mSubmission submission;
    @Column(name = "event_type")
    private String eventType;
    @Column(name = "nav_operation")
    private String navOperation;
    @Column(name = "request_message_id")
    private String requestMessageId;
    @Column(name = "response_code")
    private String responseCode;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "request_headers")
    private String requestHeaders;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "request_payload")
    private String requestPayload;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "response_headers")
    private String responseHeaders;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "response_payload")
    private String responsePayload;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "config_snapshot")
    private String configSnapshot;
    @Column(name = "created_at")
    private Instant createdAt;

    /** Inicializálja az új beküldési esemény technikai azonosítóját és létrehozási időpontját perzisztálás előtt. */
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
     * Visszaadja a(z) eseménytípus aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getEventType() { return eventType; }
    /**
     * Beállítja a(z) eseménytípus értékét a domain objektumon.
     *
     * @param eventType a rögzítendő esemény típusa
     */
    public void setEventType(String eventType) { this.eventType = eventType; }
    /**
     * Visszaadja a(z) navOperation aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getNavOperation() { return navOperation; }
    /**
     * Beállítja a(z) navOperation értékét a domain objektumon.
     *
     * @param navOperation a művelethez átadott {@code navOperation} érték
     */
    public void setNavOperation(String navOperation) { this.navOperation = navOperation; }
    /**
     * Visszaadja a(z) requestMessageId aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getRequestMessageId() { return requestMessageId; }
    /**
     * Beállítja a(z) requestMessageId értékét a domain objektumon.
     *
     * @param requestMessageId a művelethez átadott {@code requestMessageId} érték
     */
    public void setRequestMessageId(String requestMessageId) { this.requestMessageId = requestMessageId; }
    /**
     * Visszaadja a(z) responseCode aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getResponseCode() { return responseCode; }
    /**
     * Beállítja a(z) responseCode értékét a domain objektumon.
     *
     * @param responseCode a művelethez átadott {@code responseCode} érték
     */
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
    /**
     * Visszaadja a(z) requestHeaders aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getRequestHeaders() { return requestHeaders; }
    /**
     * Beállítja a(z) requestHeaders értékét a domain objektumon.
     *
     * @param requestHeaders a művelethez átadott {@code requestHeaders} érték
     */
    public void setRequestHeaders(String requestHeaders) { this.requestHeaders = requestHeaders; }
    /**
     * Visszaadja a(z) requestPayload aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getRequestPayload() { return requestPayload; }
    /**
     * Beállítja a(z) requestPayload értékét a domain objektumon.
     *
     * @param requestPayload a művelethez átadott {@code requestPayload} érték
     */
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }
    /**
     * Visszaadja a(z) responseHeaders aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getResponseHeaders() { return responseHeaders; }
    /**
     * Beállítja a(z) responseHeaders értékét a domain objektumon.
     *
     * @param responseHeaders a művelethez átadott {@code responseHeaders} érték
     */
    public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }
    /**
     * Visszaadja a(z) responsePayload aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getResponsePayload() { return responsePayload; }
    /**
     * Beállítja a(z) responsePayload értékét a domain objektumon.
     *
     * @param responsePayload a művelethez átadott {@code responsePayload} érték
     */
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
    /**
     * Visszaadja a(z) configSnapshot aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getConfigSnapshot() { return configSnapshot; }
    /**
     * Beállítja a(z) configSnapshot értékét a domain objektumon.
     *
     * @param configSnapshot a művelethez átadott {@code configSnapshot} érték
     */
    public void setConfigSnapshot(String configSnapshot) { this.configSnapshot = configSnapshot; }
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
