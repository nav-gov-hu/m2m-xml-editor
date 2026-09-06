package hu.gov.nav.xsdparsertool.web.xsdvalidation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.gov.nav.xsdparsertool.web.xsdvalidation.entity.XsdValidationErrorEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code XsdValidationErrorRepository} interfész a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface XsdValidationErrorRepository extends JpaRepository<XsdValidationErrorEntity, Long> {
    /**
     * A {@code findByRequestIdOrderByIdAsc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XsdValidationErrorEntity> findByRequestIdOrderByIdAsc(String requestId);
    /**
     * A {@code countByRequestIdAndSeverityNot} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param severity a művelet bemeneti {@code severity} értéke
     * @return a művelet feldolgozási eredménye
     */
    long countByRequestIdAndSeverityNot(String requestId, String severity);
    /**
     * A {@code deleteByRequestId} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     */
    void deleteByRequestId(String requestId);
}
