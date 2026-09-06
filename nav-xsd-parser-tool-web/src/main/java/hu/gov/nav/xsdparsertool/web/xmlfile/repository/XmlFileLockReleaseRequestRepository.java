package hu.gov.nav.xsdparsertool.web.xmlfile.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockReleaseRequestEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code XmlFileLockReleaseRequestRepository} interfész a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface XmlFileLockReleaseRequestRepository extends JpaRepository<XmlFileLockReleaseRequestEntity, Long> {
    /**
     * A {@code findByOwnerUsernameAndOwnerBrowserSessionIdAndStatusOrderByRequestedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param ownerUsername a művelet felhasználói kontextusa vagy felhasználóneve
     * @param ownerBrowserSessionId a célobjektum vagy erőforrás azonosítója
     * @param status a feldolgozás aktuális vagy beállítandó állapota
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileLockReleaseRequestEntity> findByOwnerUsernameAndOwnerBrowserSessionIdAndStatusOrderByRequestedAtDesc(String ownerUsername, String ownerBrowserSessionId, String status);
    /**
     * A {@code findByRequesterUsernameAndRequesterBrowserSessionIdOrderByRequestedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param requesterUsername a művelet bemeneti kérésadatait tartalmazó objektum
     * @param requesterBrowserSessionId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileLockReleaseRequestEntity> findByRequesterUsernameAndRequesterBrowserSessionIdOrderByRequestedAtDesc(String requesterUsername, String requesterBrowserSessionId);
    /**
     * A {@code findByXmlFileIdAndStatusOrderByRequestedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param status a feldolgozás aktuális vagy beállítandó állapota
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileLockReleaseRequestEntity> findByXmlFileIdAndStatusOrderByRequestedAtDesc(Long xmlFileId, String status);
}
