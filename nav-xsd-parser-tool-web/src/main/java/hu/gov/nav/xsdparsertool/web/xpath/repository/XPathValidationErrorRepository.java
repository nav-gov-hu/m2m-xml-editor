package hu.gov.nav.xsdparsertool.web.xpath.repository;

import hu.gov.nav.xsdparsertool.web.xpath.entity.XPathValidationErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * A(z) XPathValidationErrorRepository interface fő feladata a(z) repository rétegben megvalósított funkciók biztosítása.
 * Az osztály a repository csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public interface XPathValidationErrorRepository extends JpaRepository<XPathValidationErrorEntity, String> {
    /**
     * A {@code findByRequestIdOrderByCreatedAtAsc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XPathValidationErrorEntity> findByRequestIdOrderByCreatedAtAsc(String requestId);
    /**
     * A {@code deleteByRequestId} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     */
    void deleteByRequestId(String requestId);
}
