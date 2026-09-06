package hu.gov.nav.xsdparsertool.web.xmlfile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code XmlFileRepository} interfész a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface XmlFileRepository extends JpaRepository<XmlFileEntity, Long> {
    /**
     * A {@code existsByFileNameIgnoreCase} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    boolean existsByFileNameIgnoreCase(String fileName);
    /**
     * A {@code existsByFilePathIgnoreCase} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param filePath a feldolgozásban részt vevő fájl vagy elérési út
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    boolean existsByFilePathIgnoreCase(String filePath);
    /**
     * A {@code findByFileNameIgnoreCase} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<XmlFileEntity> findByFileNameIgnoreCase(String fileName);
    /**
     * A {@code findByArchivedFalseOrderByCreatedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileEntity> findByArchivedFalseOrderByCreatedAtDesc();
    /**
     * A {@code findByArchivedTrueOrderByCreatedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    List<XmlFileEntity> findByArchivedTrueOrderByCreatedAtDesc();
}
