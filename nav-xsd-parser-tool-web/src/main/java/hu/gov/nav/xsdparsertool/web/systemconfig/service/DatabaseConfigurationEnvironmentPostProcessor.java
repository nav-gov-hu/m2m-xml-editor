package hu.gov.nav.xsdparsertool.web.systemconfig.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A futásidejű, adatbázisban tárolható rendszerbeállításokat Spring property
 * source-ként teszi elérhetővé. A bootstrap-beállításokat szándékosan nem tölti
 * vissza az adatbázisból, mert azok már az adatbázis és a Spring infrastruktúra
 * felépítéséhez szükségesek.
 */
public class DatabaseConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Set<String> BOOTSTRAP_EXACT_KEYS = Set.of(
            "nav.xsdparsertool.database.type",
            "nav.xsdparsertool.data-directory",
            "nav.xsdparsertool.bootstrap-config-file",
            "nav.xsdparsertool.setup.completed",
            "nav.xsdparsertool.security.mode"
    );

    private static final String[] BOOTSTRAP_PREFIXES = {
            "spring.datasource.",
            "spring.jpa.",
            "spring.flyway.",
            "spring.config.",
            "server.",
            "management."
    };

    /**
     * A {@code postProcessEnvironment} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param application a művelet bemeneti {@code application} értéke
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        DataSourceProperties datasource = Binder.get(environment)
                .bind("spring.datasource", DataSourceProperties.class)
                .orElseGet(DataSourceProperties::new);
        String url = datasource.getUrl();
        String username = datasource.getUsername();
        String driver = datasource.getDriverClassName();
        if (url == null || url.isBlank()) {
            return;
        }

        try {
            if (driver != null && !driver.isBlank()) {
                Class.forName(driver);
            }
            Map<String, Object> values = new LinkedHashMap<>();
            try (Connection connection = openConnection(url, username, datasource.getPassword());
                 PreparedStatement statement = connection.prepareStatement(
                         "select config_key, config_value from system_configuration");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String key = resultSet.getString(1);
                    String value = resultSet.getString(2);
                    if (isRuntimeConfigurationKey(key) && value != null && !value.isBlank()) {
                        values.put(key, value);
                    }
                }
            }
            if (!values.isEmpty()) {
                environment.getPropertySources().addFirst(
                        new MapPropertySource("databaseSystemConfiguration", values));
            }
        } catch (Exception ignored) {
            // Első induláskor a tábla még nem feltétlenül létezik; Flyway ezt később létrehozza.
        }
    }


    /**
     * A {@code openConnection} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param url a művelet bemeneti {@code url} értéke
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param password a művelet bemeneti {@code password} értéke
     * @return a művelet feldolgozási eredménye
     * @throws java.sql.SQLException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Connection openConnection(String url, String username, String password) throws java.sql.SQLException {
        if (username == null || username.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * A {@code isRuntimeConfigurationKey} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    static boolean isRuntimeConfigurationKey(String key) {
        if (key == null || key.isBlank() || BOOTSTRAP_EXACT_KEYS.contains(key)) {
            return false;
        }
        for (String prefix : BOOTSTRAP_PREFIXES) {
            if (key.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A {@code getOrder} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
