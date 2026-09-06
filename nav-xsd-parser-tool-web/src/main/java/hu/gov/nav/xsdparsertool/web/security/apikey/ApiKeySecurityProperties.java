package hu.gov.nav.xsdparsertool.web.security.apikey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Egyszeru, kulso konfiguraciobol olvasott API kulcs beallitasok.
 */
@Component
public class ApiKeySecurityProperties {

    private volatile boolean enabled;
    private volatile String headerName;
    private volatile String apiKey;
    private volatile String principalName;

    /**
     * Létrehozza a {@code ApiKeySecurityProperties} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param enabled a művelet bemeneti {@code enabled} értéke
     * @param headerName a feloldáshoz vagy azonosításhoz használt név
     * @param apiKey a művelet bemeneti {@code apiKey} értéke
     * @param principalName a feloldáshoz vagy azonosításhoz használt név
     */
    public ApiKeySecurityProperties(
            @Value("${nav.xsdparsertool.api-key.enabled:false}") boolean enabled,
            @Value("${nav.xsdparsertool.api-key.header-name:X-API-Key}") String headerName,
            @Value("${nav.xsdparsertool.api-key.value:}") String apiKey,
            @Value("${nav.xsdparsertool.api-key.principal-name:external-api-key-client}") String principalName) {
        this.enabled = enabled;
        this.headerName = StringUtils.hasText(headerName) ? headerName.trim() : "X-API-Key";
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.principalName = StringUtils.hasText(principalName) ? principalName.trim() : "external-api-key-client";
    }

    /**
     * A {@code isEnabled} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isEnabled() {
        return enabled && StringUtils.hasText(apiKey);
    }

    /**
     * A {@code isConfiguredEnabled} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isConfiguredEnabled() {
        return enabled;
    }

    /**
     * A {@code hasApiKey} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * A {@code setEnabled} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param enabled a művelet bemeneti {@code enabled} értéke
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * A {@code getHeaderName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * A {@code setHeaderName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param headerName a feloldáshoz vagy azonosításhoz használt név
     */
    public void setHeaderName(String headerName) {
        this.headerName = StringUtils.hasText(headerName) ? headerName.trim() : "X-API-Key";
    }

    /**
     * A {@code getApiKey} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * A {@code setApiKey} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param apiKey a művelet bemeneti {@code apiKey} értéke
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    /**
     * A {@code getPrincipalName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getPrincipalName() {
        return principalName;
    }

    /**
     * A {@code setPrincipalName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param principalName a feloldáshoz vagy azonosításhoz használt név
     */
    public void setPrincipalName(String principalName) {
        this.principalName = StringUtils.hasText(principalName) ? principalName.trim() : "external-api-key-client";
    }
}
