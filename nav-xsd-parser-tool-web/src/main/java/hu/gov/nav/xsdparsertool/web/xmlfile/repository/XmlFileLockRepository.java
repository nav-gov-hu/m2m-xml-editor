package hu.gov.nav.xsdparsertool.web.xmlfile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code XmlFileLockRepository} interfész a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface XmlFileLockRepository extends JpaRepository<XmlFileLockEntity, Long> {
    /**
     * A {@code findByXmlFileId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<XmlFileLockEntity> findByXmlFileId(Long xmlFileId);
    /**
     * A {@code findByXmlFileIdAndStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param status a feldolgozás aktuális vagy beállítandó állapota
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<XmlFileLockEntity> findByXmlFileIdAndStatus(Long xmlFileId, String status);
    /**
     * A {@code findByLockTokenAndStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lockToken a művelet bemeneti {@code lockToken} értéke
     * @param status a feldolgozás aktuális vagy beállítandó állapota
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<XmlFileLockEntity> findByLockTokenAndStatus(String lockToken, String status);
}
