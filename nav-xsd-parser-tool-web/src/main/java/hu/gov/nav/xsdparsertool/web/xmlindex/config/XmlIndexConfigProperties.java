package hu.gov.nav.xsdparsertool.web.xmlindex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A külső konfiguráció kapcsolódó beállításait típusosan összefogó konfigurációs modell.
 *
 * <p>A {@code XmlIndexConfigProperties} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
@ConfigurationProperties(prefix = "nav.xsdparsertool.xml-index")
public class XmlIndexConfigProperties {
    /**
     * Űrlap szintű XML index konfigurációs fájl elérési útja.
     */
    private String configPath;

    /**
     * A {@code getConfigPath} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getConfigPath() {
        return configPath;
    }

    /**
     * A {@code setConfigPath} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param configPath a feldolgozásban részt vevő fájl vagy elérési út
     */
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}
