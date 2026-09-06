package hu.gov.nav.xsdparsertool.web.xsdvalidation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A külső konfiguráció kapcsolódó beállításait típusosan összefogó konfigurációs modell.
 *
 * <p>A {@code XsdValidationProperties} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
@ConfigurationProperties(prefix = "nav.xsdparsertool.xsd-validation")
public class XsdValidationProperties {
    private int maxErrors = 500;

    /**
     * A {@code getMaxErrors} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public int getMaxErrors() { return maxErrors; }
    /**
     * A {@code setMaxErrors} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param maxErrors a művelet bemeneti {@code maxErrors} értéke
     */
    public void setMaxErrors(int maxErrors) { this.maxErrors = maxErrors; }
}
