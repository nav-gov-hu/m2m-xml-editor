package hu.gov.nav.xsdparsertool.web.xpath.repository;

import hu.gov.nav.xsdparsertool.web.xpath.entity.XPathValidationRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * A(z) XPathValidationRequestRepository interface fő feladata a(z) repository rétegben megvalósított funkciók biztosítása.
 * Az osztály a repository csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public interface XPathValidationRequestRepository extends JpaRepository<XPathValidationRequestEntity, String> {
    /**
     * A {@code findByRequestId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<XPathValidationRequestEntity> findByRequestId(String requestId);
    /**
     * A {@code existsByRequestId} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    boolean existsByRequestId(String requestId);
    /**
     * A {@code findByRequestIdContainingIgnoreCase} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param pageable a lapozási vagy mennyiségi korlátot meghatározó érték
     * @return a feloldott vagy lekért érték
     */
    Page<XPathValidationRequestEntity> findByRequestIdContainingIgnoreCase(String requestId, Pageable pageable);
    /**
     * A {@code findFirstByXmlFileIdOrderByCreatedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<XPathValidationRequestEntity> findFirstByXmlFileIdOrderByCreatedAtDesc(Long xmlFileId);
}
