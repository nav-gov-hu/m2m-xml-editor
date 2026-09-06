package hu.gov.nav.xsdparsertool.web.database;

import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code DatabaseStatusController} osztály a web modul adatbázis-konfigurációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
public class DatabaseStatusController {

    private final DataSource dataSource;
    private final DatabaseConfigurationProperties databaseConfigurationProperties;
    private final SecurityModeProperties securityModeProperties;
    private final ObjectProvider<Flyway> flywayProvider;
    private final String datasourceUrl;
    private final boolean h2ConsoleEnabled;
    private final String h2ConsolePath;

    /**
     * Létrehozza a {@code DatabaseStatusController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param dataSource a művelet bemeneti {@code dataSource} értéke
     * @param databaseConfigurationProperties a művelethez szükséges konfigurációs adatok
     * @param securityModeProperties a művelethez szükséges konfigurációs adatok
     * @param flywayProvider a művelet bemeneti {@code flywayProvider} értéke
     * @param datasourceUrl a művelet bemeneti {@code datasourceUrl} értéke
     * @param h2ConsoleEnabled jelzi, hogy a H2 webkonzol engedélyezett-e
     * @param h2ConsolePath a H2 webkonzol konfigurált elérési útja
     */
    public DatabaseStatusController(
            DataSource dataSource,
            DatabaseConfigurationProperties databaseConfigurationProperties,
            SecurityModeProperties securityModeProperties,
            ObjectProvider<Flyway> flywayProvider,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${spring.h2.console.enabled:true}") boolean h2ConsoleEnabled,
            @Value("${spring.h2.console.path:/h2-console}") String h2ConsolePath) {
        this.dataSource = dataSource;
        this.databaseConfigurationProperties = databaseConfigurationProperties;
        this.securityModeProperties = securityModeProperties;
        this.flywayProvider = flywayProvider;
        this.datasourceUrl = datasourceUrl;
        this.h2ConsoleEnabled = h2ConsoleEnabled;
        this.h2ConsolePath = h2ConsolePath;
    }

    /**
     * A {@code databaseStatus} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @GetMapping("/api/database/status")
    public Map<String, Object> databaseStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("configuredDatabaseType", databaseConfigurationProperties.getDatabaseType().name());
        response.put("configuredSchema", databaseConfigurationProperties.getSchema());
        response.put("configuredEncoding", databaseConfigurationProperties.getEncoding());
        response.put("securityMode", securityModeProperties.getSecurityMode().name());
        response.put("standaloneUsername", securityModeProperties.getStandaloneUsername());
        response.put("datasourceUrl", maskJdbcUrl(datasourceUrl));
        response.put("h2ConsoleEnabled", h2ConsoleEnabled);
        response.put("h2ConsolePath", h2ConsolePath);

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            response.put("status", "OK");
            response.put("databaseProductName", metaData.getDatabaseProductName());
            response.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            response.put("driverName", metaData.getDriverName());
            response.put("driverVersion", metaData.getDriverVersion());
            response.put("schema", safeSchema(connection));
        } catch (Exception ex) {
            response.put("status", "ERROR");
            response.put("message", "Nem sikerült kapcsolódni az adatbázishoz. Ellenőrizd az adatbázis típusát, URL-jét, felhasználónevét, jelszavát és sémáját.");
            response.put("technicalMessage", ex.getMessage());
        }

        Flyway flyway = flywayProvider.getIfAvailable();
        if (flyway != null) {
            try {
                MigrationInfo current = flyway.info().current();
                response.put("flywayEnabled", true);
                response.put("flywayCurrentVersion", current == null ? null : String.valueOf(current.getVersion()));
                response.put("flywayCurrentDescription", current == null ? null : current.getDescription());
            } catch (Exception ex) {
                response.put("flywayEnabled", true);
                response.put("flywayStatus", "ERROR");
                response.put("flywayError", ex.getMessage());
            }
        } else {
            response.put("flywayEnabled", false);
        }

        return response;
    }

    /**
     * A {@code safeSchema} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param connection a művelet bemeneti {@code connection} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String safeSchema(Connection connection) {
        try {
            return connection.getSchema();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * A {@code maskJdbcUrl} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param url a művelet bemeneti {@code url} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String maskJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.replaceAll("(?i)(password=)[^;&]+", "$1****");
    }
}
