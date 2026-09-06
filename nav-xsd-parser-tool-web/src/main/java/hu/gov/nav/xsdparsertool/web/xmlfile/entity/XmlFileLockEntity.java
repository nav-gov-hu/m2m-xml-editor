package hu.gov.nav.xsdparsertool.web.xmlfile.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * A perzisztens állapot adatbázis-reprezentációját leíró entitás.
 *
 * <p>A {@code XmlFileLockEntity} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "xml_file_lock")
public class XmlFileLockEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "xml_file_id", nullable = false)
    private XmlFileEntity xmlFile;

    @Column(name = "locked_by", nullable = false, length = 255)
    private String lockedBy;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "lock_expires_at", nullable = false)
    private LocalDateTime lockExpiresAt;

    @Column(name = "lock_token", nullable = false, length = 100)
    private String lockToken;

    @Column(name = "lock_browser_session_id", length = 100)
    private String lockBrowserSessionId;

    @Column(name = "lock_client_ip", length = 100)
    private String lockClientIp;

    @Column(name = "lock_user_agent", length = 1000)
    private String lockUserAgent;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
     * A {@code getLockedBy} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getLockedBy() { return lockedBy; }
    /**
     * A {@code setLockedBy} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lockedBy a művelet bemeneti {@code lockedBy} értéke
     */
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    /**
     * A {@code getLockedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getLockedAt() { return lockedAt; }
    /**
     * A {@code setLockedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lockedAt a művelet bemeneti {@code lockedAt} értéke
     */
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    /**
     * A {@code getLockExpiresAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getLockExpiresAt() { return lockExpiresAt; }
    /**
     * A {@code setLockExpiresAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lockExpiresAt a művelet bemeneti {@code lockExpiresAt} értéke
     */
    public void setLockExpiresAt(LocalDateTime lockExpiresAt) { this.lockExpiresAt = lockExpiresAt; }
    /**
     * A {@code getLockToken} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getLockToken() { return lockToken; }
    /**
     * A {@code setLockToken} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lockToken a művelet bemeneti {@code lockToken} értéke
     */
    public void setLockToken(String lockToken) { this.lockToken = lockToken; }
    /**
     * A {@code getLockBrowserSessionId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getLockBrowserSessionId() { return lockBrowserSessionId; }
    /**
     * A {@code setLockBrowserSessionId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lockBrowserSessionId a célobjektum vagy erőforrás azonosítója
     */
    public void setLockBrowserSessionId(String lockBrowserSessionId) { this.lockBrowserSessionId = lockBrowserSessionId; }
    /**
     * A {@code getLockClientIp} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getLockClientIp() { return lockClientIp; }
    /**
     * A {@code setLockClientIp} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lockClientIp a művelet bemeneti {@code lockClientIp} értéke
     */
    public void setLockClientIp(String lockClientIp) { this.lockClientIp = lockClientIp; }
    /**
     * A {@code getLockUserAgent} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getLockUserAgent() { return lockUserAgent; }
    /**
     * A {@code setLockUserAgent} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lockUserAgent a művelet felhasználói kontextusa vagy felhasználóneve
     */
    public void setLockUserAgent(String lockUserAgent) { this.lockUserAgent = lockUserAgent; }
    /**
     * A {@code getStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getStatus() { return status; }
    /**
     * A {@code setStatus} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param status a feldolgozás aktuális vagy beállítandó állapota
     */
    public void setStatus(String status) { this.status = status; }
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
     * A {@code getUpdatedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /**
     * A {@code setUpdatedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param updatedAt a művelet bemeneti {@code updatedAt} értéke
     */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
