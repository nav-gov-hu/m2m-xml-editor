package hu.gov.nav.xsdparsertool.web.processing.entity;

import java.time.LocalDateTime;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code ProcessingJobEntity} osztály a web modul feldolgozási job területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "processing_job")
public class ProcessingJobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 100, unique = true)
    private String jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "xml_file_id")
    private XmlFileEntity xmlFile;

    @Column(name = "job_type", nullable = false, length = 100)
    private String jobType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "progress_message", length = 2000)
    private String progressMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "requested_cancel_at")
    private LocalDateTime requestedCancelAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    /**
     * A {@code getId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public Long getId() { return id; }
    /**
     * A {@code setId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     */
    public void setId(Long id) { this.id = id; }
    /**
     * A {@code getJobId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getJobId() { return jobId; }
    /**
     * A {@code setJobId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     */
    public void setJobId(String jobId) { this.jobId = jobId; }
    /**
     * A {@code getXmlFile} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a feldolgozási job folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public XmlFileEntity getXmlFile() { return xmlFile; }
    /**
     * A {@code setXmlFile} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a feldolgozási job folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public void setXmlFile(XmlFileEntity xmlFile) { this.xmlFile = xmlFile; }
    /**
     * A {@code getJobType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getJobType() { return jobType; }
    /**
     * A {@code setJobType} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobType a művelet bemeneti {@code jobType} értéke
     */
    public void setJobType(String jobType) { this.jobType = jobType; }
    /**
     * A {@code getStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getStatus() { return status; }
    /**
     * A {@code setStatus} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param status a feldolgozás aktuális vagy beállítandó állapota
     */
    public void setStatus(String status) { this.status = status; }
    /**
     * A {@code getProgressPercent} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public Integer getProgressPercent() { return progressPercent; }
    /**
     * A {@code setProgressPercent} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param progressPercent a művelet bemeneti {@code progressPercent} értéke
     */
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    /**
     * A {@code getProgressMessage} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getProgressMessage() { return progressMessage; }
    /**
     * A {@code setProgressMessage} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param progressMessage a művelet bemeneti {@code progressMessage} értéke
     */
    public void setProgressMessage(String progressMessage) { this.progressMessage = progressMessage; }
    /**
     * A {@code getStartedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getStartedAt() { return startedAt; }
    /**
     * A {@code setStartedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param startedAt a művelet bemeneti {@code startedAt} értéke
     */
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    /**
     * A {@code getFinishedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getFinishedAt() { return finishedAt; }
    /**
     * A {@code setFinishedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param finishedAt a művelet bemeneti {@code finishedAt} értéke
     */
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    /**
     * A {@code getRequestedCancelAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getRequestedCancelAt() { return requestedCancelAt; }
    /**
     * A {@code setRequestedCancelAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestedCancelAt a művelet bemeneti kérésadatait tartalmazó objektum
     */
    public void setRequestedCancelAt(LocalDateTime requestedCancelAt) { this.requestedCancelAt = requestedCancelAt; }
    /**
     * A {@code getErrorMessage} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getErrorMessage() { return errorMessage; }
    /**
     * A {@code setErrorMessage} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param errorMessage a művelet bemeneti {@code errorMessage} értéke
     */
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    /**
     * A {@code getCreatedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /**
     * A {@code setCreatedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param createdAt a művelet bemeneti {@code createdAt} értéke
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /**
     * A {@code getCreatedBy} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getCreatedBy() { return createdBy; }
    /**
     * A {@code setCreatedBy} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param createdBy a művelet bemeneti {@code createdBy} értéke
     */
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    /**
     * A {@code getUpdatedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /**
     * A {@code setUpdatedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param updatedAt a művelet bemeneti {@code updatedAt} értéke
     */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    /**
     * A {@code getUpdatedBy} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getUpdatedBy() { return updatedBy; }
    /**
     * A {@code setUpdatedBy} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param updatedBy a művelet bemeneti {@code updatedBy} értéke
     */
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
