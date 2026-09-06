package hu.gov.nav.xsdparsertool.web.xpath.entity;

import hu.gov.nav.xsdparsertool.web.xpath.model.CreateResultMode;
import hu.gov.nav.xsdparsertool.web.xpath.model.ResultStatus;
import hu.gov.nav.xsdparsertool.web.xpath.model.ValidatorStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;

import java.time.Instant;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code XPathValidationRequestEntity} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "xpath_validation_request", indexes = {
        @Index(name = "idx_xpath_validation_request_created_at", columnList = "created_at"),
        @Index(name = "idx_xpath_validation_request_request_id", columnList = "request_id", unique = true),
        @Index(name = "idx_xpath_validation_request_xml_file", columnList = "xml_file_id")
})
/**
 * Egy XPath-validáció teljes perzisztens kérésállapotát, bemeneti metaadatait és feldolgozási eredményét hordozó entitás.
 * Az osztály a entity csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */

public class XPathValidationRequestEntity {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "request_id", length = 18, nullable = false, unique = true)
    private String requestId;

    @Column(name = "request_timestamp_utc", nullable = false)
    private Instant requestTimestampUtc;

    @Column(name = "form_name", length = 20, nullable = false)
    private String formName;

    @Column(name = "form_version", length = 10, nullable = false)
    private String formVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "validator_status", length = 10, nullable = false)
    private ValidatorStatus validatorStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", length = 10)
    private ResultStatus resultStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "create_result_mode", length = 10, nullable = false)
    private CreateResultMode createResultMode;

    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "xml_file_id")
    private XmlFileEntity xmlFile;

    @Column(name = "xml_file_session_id", length = 100)
    private String xmlFileSessionId;

    @Column(name = "processing_job_id", length = 100)
    private String processingJobId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "result")
    private String result;

    @Column(name = "result_file_path", length = 1024)
    private String resultFilePath;

    @Column(name = "error_count")
    private Integer errorCount;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "technical_error_message")
    private String technicalErrorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;
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
 * Visszaadja a {@code requestTimestampUtc} mező aktuális értékét.
 * @return a {@code requestTimestampUtc} mező értéke
 */
    public Instant getRequestTimestampUtc() { return requestTimestampUtc; }
/**
 * Beállítja a {@code requestTimestampUtc} mező értékét.
 * @param requestTimestampUtc a beállítandó új érték
 */
    public void setRequestTimestampUtc(Instant requestTimestampUtc) { this.requestTimestampUtc = requestTimestampUtc; }
/**
 * Visszaadja a {@code formName} mező aktuális értékét.
 * @return a {@code formName} mező értéke
 */
    public String getFormName() { return formName; }
/**
 * Beállítja a {@code formName} mező értékét.
 * @param formName a beállítandó új érték
 */
    public void setFormName(String formName) { this.formName = formName; }
/**
 * Visszaadja a {@code formVersion} mező aktuális értékét.
 * @return a {@code formVersion} mező értéke
 */
    public String getFormVersion() { return formVersion; }
/**
 * Beállítja a {@code formVersion} mező értékét.
 * @param formVersion a beállítandó új érték
 */
    public void setFormVersion(String formVersion) { this.formVersion = formVersion; }
/**
 * Visszaadja a {@code validatorStatus} mező aktuális értékét.
 * @return a {@code validatorStatus} mező értéke
 */
    public ValidatorStatus getValidatorStatus() { return validatorStatus; }
/**
 * Beállítja a {@code validatorStatus} mező értékét.
 * @param validatorStatus a beállítandó új érték
 */
    public void setValidatorStatus(ValidatorStatus validatorStatus) { this.validatorStatus = validatorStatus; }
/**
 * Visszaadja a {@code resultStatus} mező aktuális értékét.
 * @return a {@code resultStatus} mező értéke
 */
    public ResultStatus getResultStatus() { return resultStatus; }
/**
 * Beállítja a {@code resultStatus} mező értékét.
 * @param resultStatus a beállítandó új érték
 */
    public void setResultStatus(ResultStatus resultStatus) { this.resultStatus = resultStatus; }
/**
 * Visszaadja a {@code createResultMode} mező aktuális értékét.
 * @return a {@code createResultMode} mező értéke
 */
    public CreateResultMode getCreateResultMode() { return createResultMode; }
/**
 * Beállítja a {@code createResultMode} mező értékét.
 * @param createResultMode a beállítandó új érték
 */
    public void setCreateResultMode(CreateResultMode createResultMode) { this.createResultMode = createResultMode; }
/**
 * Visszaadja a {@code sessionId} mező aktuális értékét.
 * @return a {@code sessionId} mező értéke
 */
    public String getSessionId() { return sessionId; }

    /**
     * A {@code getXmlFile} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public XmlFileEntity getXmlFile() { return xmlFile; }
    /**
     * A {@code setXmlFile} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public void setXmlFile(XmlFileEntity xmlFile) { this.xmlFile = xmlFile; }
    /**
     * A {@code getXmlFileSessionId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getXmlFileSessionId() { return xmlFileSessionId; }
    /**
     * A {@code setXmlFileSessionId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileSessionId a célobjektum vagy erőforrás azonosítója
     */
    public void setXmlFileSessionId(String xmlFileSessionId) { this.xmlFileSessionId = xmlFileSessionId; }
    /**
     * A {@code getProcessingJobId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getProcessingJobId() { return processingJobId; }
    /**
     * A {@code setProcessingJobId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param processingJobId a célobjektum vagy erőforrás azonosítója
     */
    public void setProcessingJobId(String processingJobId) { this.processingJobId = processingJobId; }
/**
 * Beállítja a {@code sessionId} mező értékét.
 * @param sessionId a beállítandó új érték
 */
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
/**
 * Visszaadja a {@code result} mező aktuális értékét.
 * @return a {@code result} mező értéke
 */
    public String getResult() { return result; }
/**
 * Beállítja a {@code result} mező értékét.
 * @param result a beállítandó új érték
 */
    public void setResult(String result) { this.result = result; }
/**
 * Visszaadja a {@code resultFilePath} mező aktuális értékét.
 * @return a {@code resultFilePath} mező értéke
 */
    public String getResultFilePath() { return resultFilePath; }
/**
 * Beállítja a {@code resultFilePath} mező értékét.
 * @param resultFilePath a beállítandó új érték
 */
    public void setResultFilePath(String resultFilePath) { this.resultFilePath = resultFilePath; }
/**
 * Visszaadja a {@code errorCount} mező aktuális értékét.
 * @return a {@code errorCount} mező értéke
 */
    public Integer getErrorCount() { return errorCount; }
/**
 * Beállítja a {@code errorCount} mező értékét.
 * @param errorCount a beállítandó új érték
 */
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
/**
 * Visszaadja a {@code technicalErrorMessage} mező aktuális értékét.
 * @return a {@code technicalErrorMessage} mező értéke
 */
    public String getTechnicalErrorMessage() { return technicalErrorMessage; }
/**
 * Beállítja a {@code technicalErrorMessage} mező értékét.
 * @param technicalErrorMessage a beállítandó új érték
 */
    public void setTechnicalErrorMessage(String technicalErrorMessage) { this.technicalErrorMessage = technicalErrorMessage; }
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
/**
 * Visszaadja a {@code updatedAt} mező aktuális értékét.
 * @return a {@code updatedAt} mező értéke
 */
    public Instant getUpdatedAt() { return updatedAt; }
/**
 * Beállítja a {@code updatedAt} mező értékét.
 * @param updatedAt a beállítandó új érték
 */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
/**
 * Visszaadja a {@code updatedBy} mező aktuális értékét.
 * @return a {@code updatedBy} mező értéke
 */
    public String getUpdatedBy() { return updatedBy; }
/**
 * Beállítja a {@code updatedBy} mező értékét.
 * @param updatedBy a beállítandó új érték
 */
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
