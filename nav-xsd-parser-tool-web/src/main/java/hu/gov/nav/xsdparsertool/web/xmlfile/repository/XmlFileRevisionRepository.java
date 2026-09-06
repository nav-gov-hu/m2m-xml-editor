package hu.gov.nav.xsdparsertool.web.xmlfile.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileRevisionEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code XmlFileRevisionRepository} interfész a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface XmlFileRevisionRepository extends JpaRepository<XmlFileRevisionEntity, Long> {
    /**
     * A {@code findByXmlFileIdOrderByRevisionNoDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileRevisionEntity> findByXmlFileIdOrderByRevisionNoDesc(Long xmlFileId);

    /**
     * A {@code maxRevisionNo} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Query("select coalesce(max(r.revisionNo), 0) from XmlFileRevisionEntity r where r.xmlFile.id = :xmlFileId")
    Integer maxRevisionNo(@Param("xmlFileId") Long xmlFileId);

    /**
     * A {@code countByXmlFileId} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    long countByXmlFileId(Long xmlFileId);
}
