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
 * <p>A {@code XmlFileLockReleaseRequestEntity} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Entity
@Table(name = "xml_file_lock_release_request")
public class XmlFileLockReleaseRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "xml_file_id", nullable = false)
    private XmlFileEntity xmlFile;

    @Column(name = "requester_username", nullable = false, length = 255)
    private String requesterUsername;

    @Column(name = "requester_browser_session_id", length = 100)
    private String requesterBrowserSessionId;

    @Column(name = "owner_username", nullable = false, length = 255)
    private String ownerUsername;

    @Column(name = "owner_browser_session_id", length = 100)
    private String ownerBrowserSessionId;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "response_message", length = 1000)
    private String responseMessage;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "closed_by", length = 255)
    private String closedBy;

    @Column(name = "force_closed_at")
    private LocalDateTime forceClosedAt;

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
     * A {@code getRequesterUsername} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getRequesterUsername() { return requesterUsername; }
    /**
     * A {@code setRequesterUsername} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param requesterUsername a művelet bemeneti kérésadatait tartalmazó objektum
     */
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }
    /**
     * A {@code getRequesterBrowserSessionId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getRequesterBrowserSessionId() { return requesterBrowserSessionId; }
    /**
     * A {@code setRequesterBrowserSessionId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requesterBrowserSessionId a célobjektum vagy erőforrás azonosítója
     */
    public void setRequesterBrowserSessionId(String requesterBrowserSessionId) { this.requesterBrowserSessionId = requesterBrowserSessionId; }
    /**
     * A {@code getOwnerUsername} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getOwnerUsername() { return ownerUsername; }
    /**
     * A {@code setOwnerUsername} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param ownerUsername a művelet felhasználói kontextusa vagy felhasználóneve
     */
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    /**
     * A {@code getOwnerBrowserSessionId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getOwnerBrowserSessionId() { return ownerBrowserSessionId; }
    /**
     * A {@code setOwnerBrowserSessionId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ownerBrowserSessionId a célobjektum vagy erőforrás azonosítója
     */
    public void setOwnerBrowserSessionId(String ownerBrowserSessionId) { this.ownerBrowserSessionId = ownerBrowserSessionId; }
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
     * A {@code getMessage} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getMessage() { return message; }
    /**
     * A {@code setMessage} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param message a művelet bemeneti {@code message} értéke
     */
    public void setMessage(String message) { this.message = message; }
    /**
     * A {@code getResponseMessage} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getResponseMessage() { return responseMessage; }
    /**
     * A {@code setResponseMessage} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param responseMessage a feldolgozandó vagy továbbadandó válaszobjektum
     */
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
    /**
     * A {@code getRequestedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getRequestedAt() { return requestedAt; }
    /**
     * A {@code setRequestedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestedAt a művelet bemeneti kérésadatait tartalmazó objektum
     */
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    /**
     * A {@code getRespondedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getRespondedAt() { return respondedAt; }
    /**
     * A {@code setRespondedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param respondedAt a művelet bemeneti {@code respondedAt} értéke
     */
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    /**
     * A {@code getClosedBy} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getClosedBy() { return closedBy; }
    /**
     * A {@code setClosedBy} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param closedBy a művelet bemeneti {@code closedBy} értéke
     */
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
    /**
     * A {@code getForceClosedAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public LocalDateTime getForceClosedAt() { return forceClosedAt; }
    /**
     * A {@code setForceClosedAt} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param forceClosedAt a művelet bemeneti {@code forceClosedAt} értéke
     */
    public void setForceClosedAt(LocalDateTime forceClosedAt) { this.forceClosedAt = forceClosedAt; }
}
