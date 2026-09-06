package hu.gov.nav.xsdparsertool.web.xmlfile.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileDiffEntryEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code XmlFileDiffEntryRepository} interfész a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface XmlFileDiffEntryRepository extends JpaRepository<XmlFileDiffEntryEntity, Long> {
    /**
     * A {@code findByRevisionIdOrderByIdAsc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param revisionId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileDiffEntryEntity> findByRevisionIdOrderByIdAsc(Long revisionId);
    /**
     * A {@code findByXmlFileIdOrderByIdDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileDiffEntryEntity> findByXmlFileIdOrderByIdDesc(Long xmlFileId);
}
