package hu.gov.nav.xsdparsertool.web.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A külső konfiguráció kapcsolódó beállításait típusosan összefogó konfigurációs modell.
 *
 * <p>A {@code SecurityBootstrapProperties} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
public class SecurityBootstrapProperties {

    private final boolean enabled;
    private final String username;
    private final String password;
    private final String displayName;
    private final String email;

    /**
     * Létrehozza a {@code SecurityBootstrapProperties} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param enabled a művelet bemeneti {@code enabled} értéke
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param password a művelet bemeneti {@code password} értéke
     * @param displayName a feloldáshoz vagy azonosításhoz használt név
     * @param email a művelet bemeneti {@code email} értéke
     */
    public SecurityBootstrapProperties(
            @Value("${nav.xsdparsertool.security.bootstrap-admin.enabled:false}") boolean enabled,
            @Value("${nav.xsdparsertool.security.bootstrap-admin.username:admin}") String username,
            @Value("${nav.xsdparsertool.security.bootstrap-admin.password:changeMe123}") String password,
            @Value("${nav.xsdparsertool.security.bootstrap-admin.display-name:System Administrator}") String displayName,
            @Value("${nav.xsdparsertool.security.bootstrap-admin.email:}") String email) {
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.email = email;
    }

    /**
     * A {@code isEnabled} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * A {@code getUsername} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getUsername() {
        return username;
    }

    /**
     * A {@code getPassword} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getPassword() {
        return password;
    }

    /**
     * A {@code getDisplayName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * A {@code getEmail} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getEmail() {
        return email;
    }
}
