package hu.gov.nav.xsdparsertool.web.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A külső konfiguráció kapcsolódó beállításait típusosan összefogó konfigurációs modell.
 *
 * <p>A {@code SecurityModeProperties} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
public class SecurityModeProperties {

    private final String rawMode;
    private final String standaloneUsername;
    private SecurityMode securityMode;

    /**
     * Létrehozza a {@code SecurityModeProperties} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param rawMode a művelet bemeneti {@code rawMode} értéke
     * @param standaloneUsername a művelet felhasználói kontextusa vagy felhasználóneve
     */
    public SecurityModeProperties(
            @Value("${nav.xsdparsertool.security.mode:STANDALONE}") String rawMode,
            @Value("${nav.xsdparsertool.security.standalone.username:local-user}") String standaloneUsername) {
        this.rawMode = rawMode;
        this.standaloneUsername = standaloneUsername;
    }

    /**
     * A {@code validate} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     */
    @PostConstruct
    public void validate() {
        this.securityMode = SecurityMode.parse(rawMode);
    }

    /**
     * A {@code getSecurityMode} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public SecurityMode getSecurityMode() {
        return securityMode;
    }

    /**
     * A {@code getStandaloneUsername} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getStandaloneUsername() {
        return standaloneUsername;
    }
}
