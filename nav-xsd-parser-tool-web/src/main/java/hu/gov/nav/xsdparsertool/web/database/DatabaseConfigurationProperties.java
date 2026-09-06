package hu.gov.nav.xsdparsertool.web.database;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A külső konfiguráció kapcsolódó beállításait típusosan összefogó konfigurációs modell.
 *
 * <p>A {@code DatabaseConfigurationProperties} osztály a web modul adatbázis-konfigurációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
public class DatabaseConfigurationProperties {

    private final String rawType;
    private final String schema;
    private final String encoding;
    private DatabaseType databaseType;

    /**
     * Létrehozza a {@code DatabaseConfigurationProperties} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param rawType a művelet bemeneti {@code rawType} értéke
     * @param schema a művelet bemeneti {@code schema} értéke
     * @param encoding a művelet bemeneti {@code encoding} értéke
     */
    public DatabaseConfigurationProperties(
            @Value("${nav.xsdparsertool.database.type:H2}") String rawType,
            @Value("${nav.xsdparsertool.database.schema:PUBLIC}") String schema,
            @Value("${nav.xsdparsertool.database.encoding:UTF-8}") String encoding) {
        this.rawType = rawType;
        this.schema = schema;
        this.encoding = encoding;
    }

    /**
     * A {@code validate} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     */
    @PostConstruct
    public void validate() {
        this.databaseType = DatabaseType.parse(rawType);
    }

    /**
     * A {@code getDatabaseType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    /**
     * A {@code getSchema} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getSchema() {
        return schema;
    }

    /**
     * A {@code getEncoding} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getEncoding() {
        return encoding;
    }
}
