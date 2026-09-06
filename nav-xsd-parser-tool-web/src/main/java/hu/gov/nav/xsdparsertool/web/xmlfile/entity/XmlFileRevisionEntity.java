package hu.gov.nav.xsdparsertool.web.xmlfile.entity;

import java.time.LocalDateTime;

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
 * <p>A {@code XmlFileRevisionEntity} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "xml_file_revision")
public class XmlFileRevisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "xml_file_id", nullable = false)
    private XmlFileEntity xmlFile;

    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;

    @Column(name = "save_type", nullable = false, length = 50)
    private String saveType;

    @Column(name = "target_file_path", length = 2000)
    private String targetFilePath;

    @Column(name = "backup_file_path", length = 2000)
    private String backupFilePath;

    @Column(name = "diff_summary", length = 2000)
    private String diffSummary;

    @Column(name = "change_count", nullable = false)
    private Integer changeCount = 0;

    @Column(name = "xsd_validation_requested", nullable = false)
    private Boolean xsdValidationRequested = Boolean.FALSE;

    @Column(name = "xsd_validation_status", length = 50)
    private String xsdValidationStatus;

    @Column(name = "user_note", length = 1000)
    private String userNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    /**
     * A {@code getId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public Long getId() { return id; }
    /**
     * A {@code setId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     */
    public void setId(Long id) { this.id = id; }
    /**
     * A {@code getXmlFile} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public XmlFileEntity getXmlFile() { return xmlFile; }
    /**
     * A {@code setXmlFile} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public void setXmlFile(XmlFileEntity xmlFile) { this.xmlFile = xmlFile; }
    /**
     * A {@code getRevisionNo} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public Integer getRevisionNo() { return revisionNo; }
    /**
     * A {@code setRevisionNo} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param revisionNo a művelet bemeneti {@code revisionNo} értéke
     */
    public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }
    /**
     * A {@code getSaveType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getSaveType() { return saveType; }
    /**
     * A {@code setSaveType} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param saveType a művelet bemeneti {@code saveType} értéke
     */
    public void setSaveType(String saveType) { this.saveType = saveType; }
    /**
     * A {@code getTargetFilePath} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getTargetFilePath() { return targetFilePath; }
    /**
     * A {@code setTargetFilePath} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param targetFilePath a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setTargetFilePath(String targetFilePath) { this.targetFilePath = targetFilePath; }
    /**
     * A {@code getBackupFilePath} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getBackupFilePath() { return backupFilePath; }
    /**
     * A {@code setBackupFilePath} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param backupFilePath a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setBackupFilePath(String backupFilePath) { this.backupFilePath = backupFilePath; }
    /**
     * A {@code getDiffSummary} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getDiffSummary() { return diffSummary; }
    /**
     * A {@code setDiffSummary} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param diffSummary a művelet bemeneti {@code diffSummary} értéke
     */
    public void setDiffSummary(String diffSummary) { this.diffSummary = diffSummary; }
    /**
     * A {@code getChangeCount} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public Integer getChangeCount() { return changeCount; }
    /**
     * A {@code setChangeCount} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param changeCount a művelet bemeneti {@code changeCount} értéke
     */
    public void setChangeCount(Integer changeCount) { this.changeCount = changeCount; }
    /**
     * A {@code getXsdValidationRequested} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public Boolean getXsdValidationRequested() { return xsdValidationRequested; }
    /**
     * A {@code setXsdValidationRequested} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param xsdValidationRequested a művelet bemeneti kérésadatait tartalmazó objektum
     */
    public void setXsdValidationRequested(Boolean xsdValidationRequested) { this.xsdValidationRequested = xsdValidationRequested; }
    /**
     * A {@code getXsdValidationStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getXsdValidationStatus() { return xsdValidationStatus; }
    /**
     * A {@code setXsdValidationStatus} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param xsdValidationStatus a feldolgozás aktuális vagy beállítandó állapota
     */
    public void setXsdValidationStatus(String xsdValidationStatus) { this.xsdValidationStatus = xsdValidationStatus; }
    /**
     * A {@code getUserNote} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getUserNote() { return userNote; }
    /**
     * A {@code setUserNote} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param userNote a művelet felhasználói kontextusa vagy felhasználóneve
     */
    public void setUserNote(String userNote) { this.userNote = userNote; }
    /**
     * A {@code getCreatedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /**
     * A {@code setCreatedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param createdAt a művelet bemeneti {@code createdAt} értéke
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    /**
     * A {@code getCreatedBy} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getCreatedBy() { return createdBy; }
    /**
     * A {@code setCreatedBy} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param createdBy a művelet bemeneti {@code createdBy} értéke
     */
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
