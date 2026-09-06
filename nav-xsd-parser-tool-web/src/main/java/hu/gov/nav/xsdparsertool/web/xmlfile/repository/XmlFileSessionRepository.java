package hu.gov.nav.xsdparsertool.web.xmlfile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code XmlFileSessionRepository} interfész a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface XmlFileSessionRepository extends JpaRepository<XmlFileSessionEntity, Long> {
    /**
     * A {@code findByCreatedByAndActiveTrue} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param createdBy a művelet bemeneti {@code createdBy} értéke
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileSessionEntity> findByCreatedByAndActiveTrue(String createdBy);
    /**
     * A {@code findByCreatedByAndBrowserSessionIdAndActiveTrueOrderByCreatedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param createdBy a művelet bemeneti {@code createdBy} értéke
     * @param browserSessionId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileSessionEntity> findByCreatedByAndBrowserSessionIdAndActiveTrueOrderByCreatedAtDesc(String createdBy, String browserSessionId);
    /**
     * A {@code findBySessionIdAndActiveTrue} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param sessionId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<XmlFileSessionEntity> findBySessionIdAndActiveTrue(String sessionId);
    /**
     * A {@code findFirstByXmlFileIdAndCreatedByAndActiveTrueOrderByCreatedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param createdBy a művelet bemeneti {@code createdBy} értéke
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<XmlFileSessionEntity> findFirstByXmlFileIdAndCreatedByAndActiveTrueOrderByCreatedAtDesc(Long xmlFileId, String createdBy);
    /**
     * A {@code findByXmlFileIdAndActiveTrue} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileSessionEntity> findByXmlFileIdAndActiveTrue(Long xmlFileId);
}
