package hu.gov.nav.xsdparsertool.web.xpath.entity;

import hu.gov.nav.xsdparsertool.web.xpath.model.ResultStatus;
import hu.gov.nav.xsdparsertool.web.xpath.model.ValidatorStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code XPathValidationRequestJournalEntity} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "xpath_validation_request_journal", indexes = {
        @Index(name = "idx_xpath_validation_request_journal_request_id", columnList = "request_id")
})
/**
 * A XPath-validációs kérés életciklusának egy naplózott állapot- vagy feldolgozási eseményét reprezentáló entitás.
 * Az osztály a entity csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */

public class XPathValidationRequestJournalEntity {
    @Id
    @Column(length = 36, nullable = false)
    private String id;
    @Column(name = "request_entity_id", length = 36, nullable = false)
    private String requestEntityId;
    @Column(name = "request_id", length = 18, nullable = false)
    private String requestId;
    @Column(name = "event_timestamp_utc", nullable = false)
    private Instant eventTimestampUtc;
    @Enumerated(EnumType.STRING)
    @Column(name = "old_validator_status", length = 10)
    private ValidatorStatus oldValidatorStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_validator_status", length = 10)
    private ValidatorStatus newValidatorStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "old_result_status", length = 10)
    private ResultStatus oldResultStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_result_status", length = 10)
    private ResultStatus newResultStatus;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "message")
    private String message;
    @Column(name = "session_id", length = 64)
    private String sessionId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "created_by", length = 64)
    private String createdBy;
/**
 * Visszaadja a {@code id} mező aktuális értékét.
 * @return a {@code id} mező értéke
 */
    public String getId() { return id; }
/**
 * Beállítja a {@code id} mező értékét.
 * @param id a beállítandó új érték
 */
    public void setId(String id) { this.id = id; }
/**
 * Visszaadja a {@code requestEntityId} mező aktuális értékét.
 * @return a {@code requestEntityId} mező értéke
 */
    public String getRequestEntityId() { return requestEntityId; }
/**
 * Beállítja a {@code requestEntityId} mező értékét.
 * @param requestEntityId a beállítandó új érték
 */
    public void setRequestEntityId(String requestEntityId) { this.requestEntityId = requestEntityId; }
/**
 * Visszaadja a {@code requestId} mező aktuális értékét.
 * @return a {@code requestId} mező értéke
 */
    public String getRequestId() { return requestId; }
/**
 * Beállítja a {@code requestId} mező értékét.
 * @param requestId a beállítandó új érték
 */
    public void setRequestId(String requestId) { this.requestId = requestId; }
/**
 * Visszaadja a {@code eventTimestampUtc} mező aktuális értékét.
 * @return a {@code eventTimestampUtc} mező értéke
 */
    public Instant getEventTimestampUtc() { return eventTimestampUtc; }
/**
 * Beállítja a {@code eventTimestampUtc} mező értékét.
 * @param eventTimestampUtc a beállítandó új érték
 */
    public void setEventTimestampUtc(Instant eventTimestampUtc) { this.eventTimestampUtc = eventTimestampUtc; }
/**
 * Visszaadja a {@code oldValidatorStatus} mező aktuális értékét.
 * @return a {@code oldValidatorStatus} mező értéke
 */
    public ValidatorStatus getOldValidatorStatus() { return oldValidatorStatus; }
/**
 * Beállítja a {@code oldValidatorStatus} mező értékét.
 * @param oldValidatorStatus a beállítandó új érték
 */
    public void setOldValidatorStatus(ValidatorStatus oldValidatorStatus) { this.oldValidatorStatus = oldValidatorStatus; }
/**
 * Visszaadja a {@code newValidatorStatus} mező aktuális értékét.
 * @return a {@code newValidatorStatus} mező értéke
 */
    public ValidatorStatus getNewValidatorStatus() { return newValidatorStatus; }
/**
 * Beállítja a {@code newValidatorStatus} mező értékét.
 * @param newValidatorStatus a beállítandó új érték
 */
    public void setNewValidatorStatus(ValidatorStatus newValidatorStatus) { this.newValidatorStatus = newValidatorStatus; }
/**
 * Visszaadja a {@code oldResultStatus} mező aktuális értékét.
 * @return a {@code oldResultStatus} mező értéke
 */
    public ResultStatus getOldResultStatus() { return oldResultStatus; }
/**
 * Beállítja a {@code oldResultStatus} mező értékét.
 * @param oldResultStatus a beállítandó új érték
 */
    public void setOldResultStatus(ResultStatus oldResultStatus) { this.oldResultStatus = oldResultStatus; }
/**
 * Visszaadja a {@code newResultStatus} mező aktuális értékét.
 * @return a {@code newResultStatus} mező értéke
 */
    public ResultStatus getNewResultStatus() { return newResultStatus; }
/**
 * Beállítja a {@code newResultStatus} mező értékét.
 * @param newResultStatus a beállítandó új érték
 */
    public void setNewResultStatus(ResultStatus newResultStatus) { this.newResultStatus = newResultStatus; }
/**
 * Visszaadja a {@code message} mező aktuális értékét.
 * @return a {@code message} mező értéke
 */
    public String getMessage() { return message; }
/**
 * Beállítja a {@code message} mező értékét.
 * @param message a beállítandó új érték
 */
    public void setMessage(String message) { this.message = message; }
/**
 * Visszaadja a {@code sessionId} mező aktuális értékét.
 * @return a {@code sessionId} mező értéke
 */
    public String getSessionId() { return sessionId; }
/**
 * Beállítja a {@code sessionId} mező értékét.
 * @param sessionId a beállítandó új érték
 */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
/**
 * Visszaadja a {@code createdAt} mező aktuális értékét.
 * @return a {@code createdAt} mező értéke
 */
    public Instant getCreatedAt() { return createdAt; }
/**
 * Beállítja a {@code createdAt} mező értékét.
 * @param createdAt a beállítandó új érték
 */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
/**
 * Visszaadja a {@code createdBy} mező aktuális értékét.
 * @return a {@code createdBy} mező értéke
 */
    public String getCreatedBy() { return createdBy; }
/**
 * Beállítja a {@code createdBy} mező értékét.
 * @param createdBy a beállítandó új érték
 */
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
