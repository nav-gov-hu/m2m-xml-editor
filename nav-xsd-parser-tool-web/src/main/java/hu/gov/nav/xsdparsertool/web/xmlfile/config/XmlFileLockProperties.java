package hu.gov.nav.xsdparsertool.web.xmlfile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A külső konfiguráció kapcsolódó beállításait típusosan összefogó konfigurációs modell.
 *
 * <p>A {@code XmlFileLockProperties} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
@ConfigurationProperties(prefix = "nav.xsdparsertool.xml-file.lock")
public class XmlFileLockProperties {
    private long timeoutMinutes = 30L;
    private long renewMinutes = 30L;

    /**
     * A {@code getTimeoutMinutes} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public long getTimeoutMinutes() { return timeoutMinutes; }
    /**
     * A {@code setTimeoutMinutes} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param timeoutMinutes a művelet bemeneti {@code timeoutMinutes} értéke
     */
    public void setTimeoutMinutes(long timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
    /**
     * A {@code getRenewMinutes} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public long getRenewMinutes() { return renewMinutes; }
    /**
     * A {@code setRenewMinutes} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param renewMinutes a művelet bemeneti {@code renewMinutes} értéke
     */
    public void setRenewMinutes(long renewMinutes) { this.renewMinutes = renewMinutes; }
}
