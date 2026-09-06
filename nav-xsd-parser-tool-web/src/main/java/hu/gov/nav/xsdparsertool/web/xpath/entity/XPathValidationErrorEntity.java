package hu.gov.nav.xsdparsertool.web.xpath.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code XPathValidationErrorEntity} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "xpath_validation_error", indexes = {
        @Index(name = "idx_xpath_validation_error_request_id", columnList = "request_id")
})
/**
 * Egy XPath- vagy XSD-validáció során keletkezett strukturált hiba perzisztens reprezentációja.
 * Az osztály a entity csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */

public class XPathValidationErrorEntity {
    @Id
    @Column(length = 36, nullable = false)
    private String id;
    @Column(name = "request_entity_id", length = 36, nullable = false)
    private String requestEntityId;
    @Column(name = "request_id", length = 18, nullable = false)
    private String requestId;
    @Column(name = "error_code", length = 64)
    private String errorCode;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "error_message")
    private String errorMessage;
    @Column(name = "severity", length = 32)
    private String severity;
    @Column(name = "dynamic_page_index", length = 64)
    private String dynamicPageIndex;
    @Column(name = "element_id", length = 255)
    private String elementId;
    @Column(name = "rule_id", length = 64)
    private String ruleId;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "path")
    private String path;
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
 * Visszaadja a {@code errorCode} mező aktuális értékét.
 * @return a {@code errorCode} mező értéke
 */
    public String getErrorCode() { return errorCode; }
/**
 * Beállítja a {@code errorCode} mező értékét.
 * @param errorCode a beállítandó új érték
 */
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
/**
 * Visszaadja a {@code errorMessage} mező aktuális értékét.
 * @return a {@code errorMessage} mező értéke
 */
    public String getErrorMessage() { return errorMessage; }
/**
 * Beállítja a {@code errorMessage} mező értékét.
 * @param errorMessage a beállítandó új érték
 */
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
/**
 * Visszaadja a {@code severity} mező aktuális értékét.
 * @return a {@code severity} mező értéke
 */
    public String getSeverity() { return severity; }
/**
 * Beállítja a {@code severity} mező értékét.
 * @param severity a beállítandó új érték
 */
    public void setSeverity(String severity) { this.severity = severity; }
/**
 * Visszaadja a {@code dynamicPageIndex} mező aktuális értékét.
 * @return a {@code dynamicPageIndex} mező értéke
 */
    public String getDynamicPageIndex() { return dynamicPageIndex; }
/**
 * Beállítja a {@code dynamicPageIndex} mező értékét.
 * @param dynamicPageIndex a beállítandó új érték
 */
    public void setDynamicPageIndex(String dynamicPageIndex) { this.dynamicPageIndex = dynamicPageIndex; }
/**
 * Visszaadja a {@code elementId} mező aktuális értékét.
 * @return a {@code elementId} mező értéke
 */
    public String getElementId() { return elementId; }
/**
 * Beállítja a {@code elementId} mező értékét.
 * @param elementId a beállítandó új érték
 */
    public void setElementId(String elementId) { this.elementId = elementId; }
/**
 * Visszaadja a {@code ruleId} mező aktuális értékét.
 * @return a {@code ruleId} mező értéke
 */
    public String getRuleId() { return ruleId; }
/**
 * Beállítja a {@code ruleId} mező értékét.
 * @param ruleId a beállítandó új érték
 */
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
/**
 * Visszaadja a {@code path} mező aktuális értékét.
 * @return a {@code path} mező értéke
 */
    public String getPath() { return path; }
/**
 * Beállítja a {@code path} mező értékét.
 * @param path a beállítandó új érték
 */
    public void setPath(String path) { this.path = path; }
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
