package hu.gov.nav.xsdparsertool.web.xpath.repository;

import hu.gov.nav.xsdparsertool.web.xpath.entity.XPathValidationRequestJournalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * A(z) XPathValidationRequestJournalRepository interface fő feladata a(z) repository rétegben megvalósított funkciók biztosítása.
 * Az osztály a repository csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public interface XPathValidationRequestJournalRepository extends JpaRepository<XPathValidationRequestJournalEntity, String> {
    /**
     * A {@code findByRequestIdOrderByEventTimestampUtcDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XPathValidationRequestJournalEntity> findByRequestIdOrderByEventTimestampUtcDesc(String requestId);
}
